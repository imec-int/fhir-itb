package be.smals.vas.integrations.helper;

import static be.smals.vas.integrations.helper.utils.SneakyThrowUtil.sneakyThrow;
import static com.nimbusds.jose.JWEAlgorithm.DIR;
import static java.time.Instant.now;

import be.smals.vas.integrations.helper.PseudonymInTransit.PseudonymInTransitImpl;
import be.smals.vas.integrations.helper.PseudonymInTransitFactory.PseudonymInTransitFactoryImpl;
import be.smals.vas.integrations.helper.TransitInfo.TransitInfoImpl;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.DirectEncrypter;
import java.math.BigInteger;
import java.util.Base64;
import java.util.Map;
import org.bouncycastle.math.ec.ECPoint;

public sealed interface PseudonymAtRest extends Pseudonym permits PseudonymAtRest.PseudonymAtRestImpl {

  /**
   * Convert this {@link PseudonymAtRest} into a {@link PseudonymInTransit} for the given domain.
   *
   * @param toDomainKey the target domain for the returned {@link PseudonymInTransit}
   * @return a {@link PseudonymInTransit} for the given domain, matching this {@link PseudonymAtRest}
   */
  PseudonymInTransit convertTo(final String toDomainKey);

  /**
   * Create a pseudonym in transit from a pseudonym at rest.
   *
   * <p>The scalar in TransitInfo is encoded Base64.</p>
   *
   * @return Pseudonym in transit with x and y blinded and scalar in transitInfo encrypted
   */
  PseudonymInTransit createPseudonymInTransit();

  final class PseudonymAtRestImpl extends Pseudonym.PseudonymImpl implements PseudonymAtRest {

    PseudonymAtRestImpl(final ECPoint ecPoint, final Domain domain) {
      super(ecPoint, domain);
    }

    @Override
    public PseudonymInTransit convertTo(final String toDomainKey) {
      final var random = domain.createRandom();
      final var blindedPseudonym = new PseudonymImpl(ecPoint.multiply(random).normalize(), domain);
      final var payload = domain.createPayload(blindedPseudonym);
      final var rawResponse = domain.pseudonymisationClient().convertTo(domain.key(), toDomainKey, payload);
      final var targetDomain = domain.pseudonymisationHelper().getDomain(toDomainKey);
      return ((PseudonymInTransitFactoryImpl) targetDomain.pseudonymInTransitFactory()).fromRawResponse(rawResponse, random);
    }

    @Override
    public PseudonymInTransit createPseudonymInTransit() {
      final var random = domain.createRandom();
      final var randomModInverse = random.modInverse(ecPoint.getCurve().getOrder());
      final var blinded = new PseudonymImpl(ecPoint.multiply(randomModInverse).normalize(), domain);
      final var transitInfo = createTransitInfo(random);
      final var transitInfoEncrypted = encryptTransitInfo(transitInfo);
      return new PseudonymInTransitImpl(
          blinded,
          transitInfoEncrypted,
          (long) transitInfo.get("iat"),
          (long) transitInfo.get("exp"));
    }

    private Map<String, Object> createTransitInfo(final BigInteger scalar) {
      final var currentTime = now();
      return Map.of(
          "iat", currentTime.getEpochSecond(),
          "exp", currentTime.plus(domain.inTransitTtl()).getEpochSecond(),
          "scalar", Base64.getEncoder().encodeToString(scalar.toByteArray()));
    }

    /**
     * Encrypt a transitInfo and return a JWECompact encrypted where payload is transitInfo.
     * The JWE Algo is DIR and encryption method A256GCM
     *
     * @param transitInfo the transitInfo
     * @return the transitInfo in jweCompact format
     */
    private TransitInfo encryptTransitInfo(final Map<String, Object> transitInfo) {
      try {
        final var activeKid = domain.activeKid();
        final var secretKey = domain.getSecretKey(activeKid);
        final var jweHeader = new JWEHeader.Builder(DIR, domain.activeKeyEncryptionMethod())
                                  .keyID(activeKid)
                                  .customParam("aud", domain.audience())
                                  .build();
        final var jweCompact = new JWEObject(jweHeader, new Payload(transitInfo));
        // It should never happen if the domain is refreshed often enough
        if (secretKey == null) {
          throw new IllegalArgumentException("SecretKey with kid '" + activeKid + "' not found: " +
                                             "is your user allowed to get secret keys for the domain `" + domain.key() + "`?");
        }
        jweCompact.encrypt(new DirectEncrypter(secretKey));
        return new TransitInfoImpl(this.domain, jweCompact);
      } catch (final JOSEException e) {
        return sneakyThrow(e);
      }
    }
  }
}
