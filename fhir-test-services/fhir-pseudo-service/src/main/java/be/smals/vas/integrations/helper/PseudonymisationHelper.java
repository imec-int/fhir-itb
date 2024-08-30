package be.smals.vas.integrations.helper;

import static be.smals.vas.integrations.helper.internal.JWEJson.parse;
import static be.smals.vas.integrations.helper.utils.SneakyThrowUtil.sneakyThrow;
import static com.nimbusds.jose.util.JSONObjectUtils.toJSONString;
import static java.lang.Boolean.TRUE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.SECONDS;

import be.smals.vas.integrations.helper.Domain.DomainImpl;
import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.shaded.gson.Gson;
import com.nimbusds.jose.shaded.gson.GsonBuilder;
import java.net.URI;
import java.security.SecureRandom;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.stream.Stream;
import javax.crypto.SecretKey;
import org.bouncycastle.crypto.CryptoServicesRegistrar;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Library provided for the Pseudonymisation of data following the requirements exposed in the eHealth Cookbook
 * for pseudonymisation of identifiers or AES keys.
 * <p>
 * TODO: Add @see with URL of the internal VAS documentation for pseudonymization
 *
 * @see <a href="https://portal-acpt.api.ehealth.fgov.be/index.php?option=com_apiportal&view=apitester&usage=api&apitab=tests&apiName=Pseudonymisation&apiId=aed39ef2-0dc9-466f-b9b4-555444f0789b&managerId=1&type=rest&apiVersion=1.0&Itemid=158&swaggerVersion=3.0">eHealth Portal - Pseudonymisation</a>
 */
public final class PseudonymisationHelper {

  /**
   * The curve defined for the pseudonymisation by eHealth is the <a href="https://csrc.nist.gov/Projects/elliptic-curve-cryptography">NIST Curse P-521</a>.
   */
  private static final Gson GSON =
      new GsonBuilder()
          .disableHtmlEscaping()
          .create();

  private static final Logger log = LoggerFactory.getLogger(PseudonymisationHelper.class);

  private final String domainKey;
  private final PseudonymisationClient pseudonymisationClient;
  private final URI jwksUrl;
  private final JwksClient jwksClient;
  private final PrivateKeySupplier privateKeySupplier;
  private final SecureRandom secureRandom;
  private final Semaphore lock;
  private final Map<String, DomainImpl> additionalDomains;
  private volatile DomainImpl domain;
  private volatile Instant nextAllowedDomainRefresh;
  private volatile boolean initialized;

  /**
   * @param domainKey              The key of the pseudonymisation domain.
   * @param additionalDomains      Additional domains you want to convert from/to (examples: ehealth_v1, vasseclog_v1).
   * @param jwksUrl                The JWKS URL matching the encryption keys returned by the {@link PrivateKeySupplier}.
   * @param pseudonymisationClient The {@link PseudonymisationClient} to use to make calls to eHealth pseudonymisation service.
   * @param jwksClient             The {@link JwksClient} to use to retrieve the JWKS.
   * @param privateKeySupplier     The {@link PrivateKeySupplier} to use to decrypt the secret keys of the domain or to sign the JWTs.
   */
  public PseudonymisationHelper(final String domainKey,
                                Set<String> additionalDomains,
                                final URI jwksUrl,
                                final PseudonymisationClient pseudonymisationClient,
                                final JwksClient jwksClient,
                                final PrivateKeySupplier privateKeySupplier) {
    this.domainKey = domainKey;
    this.jwksUrl = jwksUrl;
    this.pseudonymisationClient = pseudonymisationClient;
    this.jwksClient = jwksClient;
    this.privateKeySupplier = privateKeySupplier;
    secureRandom = CryptoServicesRegistrar.getSecureRandom();
    lock = new Semaphore(1);
    nextAllowedDomainRefresh = Instant.MIN;
    Stream.of(Map.entry("jwksUrl", Optional.ofNullable(jwksUrl)),
              Map.entry("jwksClient", Optional.ofNullable(jwksClient)),
              Map.entry("privateKeySupplier", Optional.ofNullable(privateKeySupplier)))
          .forEach(property -> {
            if (property.getValue().isEmpty()) {
              log.warn("`" + property.getKey() + "` is null: this PseudonymisationHelper will not be able to encrypt nor decrypt any transit info");
            }
          });
    if (additionalDomains == null) {
      additionalDomains = new HashSet<>(0);
    }
    this.additionalDomains = additionalDomains.stream().collect(HashMap::new, (map, s) -> map.put(s, null), HashMap::putAll);
    try {
      refreshDomains();
    } catch (final Exception e) {
      log.error("Error during the initialization of the domain(s): the helper will not be usable until it is initialized.", e);
    }
  }

  /**
   * Returns the Domain.
   *
   * <p>
   * Please call this method everytime you need the domain: DO NOT CACHE IT !
   * You can keep the returned Domain in a local variable during the processing of a request but do not reuse it after that.
   * </p>
   *
   * @return The Domain.
   */
  public Domain getDomain() {
    if (domain == null) {
      refreshDomainAsynchronously();
      throw new IllegalStateException("The domain `" + domainKey + "` is not yet loaded");
    }
    return domain;
  }

  public Domain getDomain(final String domainKey) {
    if (domainKey.equals(this.domainKey)) {
      return getDomain();
    }
    return Optional.ofNullable(additionalDomains.get(domainKey))
                   .orElseThrow(() -> {
                     if (additionalDomains.containsKey(domainKey)) {
                       refreshDomainAsynchronously();
                       throw new IllegalStateException("The domain `" + domainKey + "` is not yet loaded");
                     } else {
                       return new IllegalArgumentException("The domain `" + domainKey +
                                                           "` is unknown: please give it to the constructor of PseudonymisationHelper.");
                     }
                   });
  }

  public void refreshDomains() {
    final var lockAcquired = lock.tryAcquire();
    try {
      if (lockAcquired) {
        final var now = now();
        if (now.isAfter(nextAllowedDomainRefresh)) {
          final ExecutorService executor = Executors.newFixedThreadPool(additionalDomains.size() + 1);
          try {
            final List<Future<DomainImpl>> futures = new ArrayList<>();
            futures.add(executor.submit(() -> createDomain(pseudonymisationClient.getDomain(domainKey))));
            additionalDomains.keySet().forEach(
                domainKey -> futures.add(executor.submit(() -> createDomain(pseudonymisationClient.getDomain(domainKey)))));
            executor.shutdown();
            for (final Future<DomainImpl> future : futures) {
              final var domain = future.get();
              if (domain.key().equals(this.domainKey)) {
                this.domain = domain;
              } else {
                additionalDomains.put(domain.key(), domain); // Traitement du résultat
              }
            }
            initialized = true;
          } catch (final InterruptedException | ExecutionException e) {
            if (initialized) {
              // If the domains are already initialized, wait 1 minute before to allow new reload to prevent DoS
              nextAllowedDomainRefresh = now.plus(1, MINUTES);
            } else {
              // If the domains are not initialized, wait 5 seconds before to allow new reload quickly while keeping some DoS protection
              nextAllowedDomainRefresh = now.plus(5, SECONDS);
            }
            sneakyThrow(e);
          }
          nextAllowedDomainRefresh = now.plus(1, MINUTES);
        }
      }
    } finally {
      if (lockAcquired) {
        lock.release();
      }
    }
  }

  void refreshDomainAsynchronously() {
    final ExecutorService executor = Executors.newSingleThreadExecutor();
    executor.submit(this::refreshDomains);
    executor.shutdown();
  }

  private DomainImpl createDomain(final String domain) {
    try {
      String activeKid = null;
      EncryptionMethod activeKeyAlgorithm = null;
      final var jku = jwksUrl == null ? null : jwksUrl.toString();
      final var jwkSet = jwksUrl == null ? null : JWKSet.parse(jwksClient.getJwks(jwksUrl));
      final var parsedEHealthDomain = GSON.fromJson(domain, Map.class);
      @SuppressWarnings("unchecked")
      final var secretKeys = (List<Map<String, Object>>) parsedEHealthDomain.get("secretKeys");
      final var jwes = new HashMap<String, SecretKey>(secretKeys.size());
      for (final Map<String, Object> secretKey : secretKeys) {
        try {
          final var kid = (String) secretKey.get("kid");
          @SuppressWarnings("unchecked")
          final var parsedJwe = parse(toJSONString((Map<String, Object>) secretKey.get("encoded")));
          final var isActiveKid = TRUE.equals(secretKey.get("active"));
          if (isActiveKid) {
            activeKid = kid;
          }
          if (jku != null) {
            final var unknownRecipients = parsedJwe.recipients().stream().filter(recipient -> !jku.equals(recipient.header().get("jku"))).toList();
            parsedJwe.recipients().removeAll(unknownRecipients);
            if (!parsedJwe.recipients().isEmpty()) {
              final var privateKid = parsedJwe.recipients().get(0).kid();
              final var privateKey = privateKeySupplier.getByHash(jwkSet.getKeyByKeyId(privateKid).getX509CertSHA256Thumbprint().toString());
              final var jwk = JWK.parse(new String(parsedJwe.decryptCipher(privateKid, privateKey), UTF_8));
              final var algName = jwk.getAlgorithm().getName();
              jwes.put(kid, ((OctetSequenceKey) jwk).toSecretKey(algName));
              if (isActiveKid) {
                activeKeyAlgorithm = EncryptionMethod.parse(algName);
              }
            }
          }
        } catch (final ParseException e) {
          throw new RuntimeException(e);
        }
      }
      final var crv = (String) parsedEHealthDomain.get("crv");
      final var curve = ECNamedCurveTable.getParameterSpec(crv).getCurve();
      final var bufferSize = ((Number) parsedEHealthDomain.get("bufferSize")).intValue();
      return new DomainImpl((String) parsedEHealthDomain.get("domain"),
                            crv,
                            curve,
                            (String) parsedEHealthDomain.get("audience"),
                            bufferSize,
                            jwes,
                            activeKid,
                            activeKeyAlgorithm,
                            Duration.parse((String) parsedEHealthDomain.get("timeToLiveInTransit")),
                            this,
                            pseudonymisationClient,
                            secureRandom
      );
    } catch (final Exception e) {
      return sneakyThrow(e);
    }
  }
}
