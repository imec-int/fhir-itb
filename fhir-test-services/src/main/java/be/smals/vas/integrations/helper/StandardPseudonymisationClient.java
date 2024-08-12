package be.smals.vas.integrations.helper;

import static be.smals.vas.integrations.helper.utils.SneakyThrowUtil.sneakyThrow;
import static com.nimbusds.jose.JWSAlgorithm.RS256;
import static com.nimbusds.jwt.JWTClaimNames.EXPIRATION_TIME;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.UUID.randomUUID;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.shaded.gson.JsonElement;
import com.nimbusds.jose.shaded.gson.JsonObject;
import com.nimbusds.jose.shaded.gson.JsonParser;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.SignedJWT;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.security.PrivateKey;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.Semaphore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link PseudonymisationClient} implementation that uses JDK HttpClient.
 * <p>
 * Use {@link Builder} to create an instance.
 */
public class StandardPseudonymisationClient implements PseudonymisationClient {

  private static final Logger log = LoggerFactory.getLogger(StandardPseudonymisationClient.class);
  private static final String ACCEPT = "Accept";
  private static final String X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";
  private static final String APPLICATION_JSON = "application/json";
  private static final String AUTHORIZATION = "Authorization";
  private static final String BEARER = "Bearer ";
  private static final String CONTENT_TYPE = "Content-Type";
  private static final String FROM = "From";
  private static final String USER_AGENT = "User-Agent";
  private final String domainsUrl;
  private final String realmUrl;
  // https://api-int.ehealth.fgov.be/auth/realms/M2M/protocol/openid-connect
  private final String openIdConnectUrl;
  private final String clientId;
  private final String userAgent;
  private final String fromHeaderValue;
  private final HttpClient httpClient;
  private final PrivateKeySupplier privateKeySupplier;
  private final String authenticationAlias;
  private AccessTokenHolder accessTokenHolder;
  private final Semaphore lock;

  private StandardPseudonymisationClient(final String pseudoUrl,
                                         final String realmUrl,
                                         final String clientId,
                                         final String fromHeaderValue,
                                         final PrivateKeySupplier privateKeySupplier,
                                         final String authenticationAlias) {
    this.domainsUrl = appendSlashIfMissing(pseudoUrl) + "domains/";
    this.realmUrl = realmUrl;
    this.openIdConnectUrl = appendSlashIfMissing(realmUrl) + "protocol/openid-connect/";
    this.clientId = clientId;
    this.userAgent = "vas-integrations-pseudonymisation/" + this.getClass().getPackage().getImplementationVersion();
    this.fromHeaderValue = (fromHeaderValue == null || fromHeaderValue.isBlank()) ? null : fromHeaderValue;
    this.httpClient = HttpClient.newBuilder().build();
    this.privateKeySupplier = privateKeySupplier;
    this.authenticationAlias = authenticationAlias == null ? "authentication" : authenticationAlias;
    lock = new Semaphore(1);
    if (privateKeySupplier != null) {
      assertNonNull(privateKeySupplier.getByAlias(authenticationAlias));
    } else {
      log.warn("`privateKeySupplier` is null: no calls to eHealth is possible");
    }
    if (clientId == null || clientId.isBlank()) {
      log.warn("`clientId` is null or blank: no calls to eHealth is possible");
    }
  }

  @Override
  public String getDomain(final String domainKey) {
    return sendRequest(domainsUrl + domainKey, null);
  }

  @Override
  public String identify(final String domainKey, final String payload) {
    return sendRequest(domainsUrl + domainKey + "/identify", payload);
  }

  @Override
  public String pseudonymize(final String domainKey, final String payload) {
    return sendRequest(domainsUrl + domainKey + "/pseudonymize", payload);
  }

  @Override
  public String convertTo(final String fromDomainKey, final String toDomainKey, final String payload) {
    return sendRequest(domainsUrl + fromDomainKey + "/convertTo/" + toDomainKey, payload);
  }

  private String sendRequest(final String url, final String payload) {
    try {
      final var requestBuilder = httpRequestBuilder(APPLICATION_JSON, APPLICATION_JSON, accessToken()).uri(URI.create(url));
      if (payload == null) {
        requestBuilder.GET();
      } else {
        requestBuilder.POST(BodyPublishers.ofString(payload));
      }
      final var response = httpClient.send(requestBuilder.build(), BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        throw new RuntimeException("GET " + url + " returned code " + response.statusCode() + ": " + response.body());
      }
      return response.body();
    } catch (final IOException | InterruptedException e) {
      return sneakyThrow(e);
    }
  }

  private HttpRequest.Builder httpRequestBuilder(final String contentType, final String accept, final String accessToken) {
    final var builder = HttpRequest.newBuilder()
                                   .header(CONTENT_TYPE, contentType)
                                   .header(USER_AGENT, userAgent);
    if (fromHeaderValue != null) {builder.header(FROM, fromHeaderValue);}
    if (accept != null) {builder.header(ACCEPT, accept);}
    if (accessToken != null) {builder.header(AUTHORIZATION, BEARER + accessToken);}
    return builder;
  }

  private String accessToken() {
    if (accessTokenHolder == null || accessTokenHolder.isTokenExpired()) {
      final var lockAcquired = lock.tryAcquire();
      try {
        if (lockAcquired) {
          final HttpResponse<String> response;
          BodyPublisher bodyPublisher = BodyPublishers.ofString(createForm(
              Map.of("client_id", clientId,
                  "grant_type", "client_credentials",
                  "client_assertion", signedJWT(),
                  "client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer")));
          response = httpClient.send(
              httpRequestBuilder(X_WWW_FORM_URLENCODED, null, null)
                  .uri(URI.create(openIdConnectUrl + "token"))
                  .POST(bodyPublisher)
                  .build(), BodyHandlers.ofString());
          // TODO: check status code
          response.statusCode();
          JsonElement accessToken = ((JsonObject) JsonParser.parseString(response.body())).get("access_token");
          if (accessToken == null) {
            throw new RuntimeException("No `access_token` in the response from IAM Connect. Response body: " + response.body());
          }
          accessTokenHolder = new AccessTokenHolder(accessToken.getAsString());
        } else {
          // If the lock has not been acquired, wait for the thread holding the lock to unlock it.
          lock.acquire();
        }
      } catch (final IOException | InterruptedException e) {
        sneakyThrow(e);
      } finally {
        if (lock.availablePermits() == 0) {
          lock.release();
        }
      }
    }
    return accessTokenHolder.token;
  }

  private static class AccessTokenHolder {

    private final String token;
    private final Instant exp;

    private AccessTokenHolder(final String token) {
      this.token = token;
      try {
        final var parsedJwt = JWTParser.parse(token);
        exp = Optional.ofNullable(parsedJwt.getJWTClaimsSet().getClaim(EXPIRATION_TIME))
                      .filter(o -> o instanceof Date)
                      .map(date -> ((Date) date).toInstant())
                      .orElse(null);
      } catch (final ParseException e) {
        throw new RuntimeException(e);
      }
      if (exp == null) {
        throw new RuntimeException("`exp` missing in the access token returned by IAM Connect");
      }
    }

    boolean isTokenExpired() {
      return exp.isBefore(now().plusSeconds(30));
    }
  }

  private static String appendSlashIfMissing(final String url) {
    if (url.endsWith("/")) {
      return url;
    }
    return url + "/";
  }

  private static String createForm(final Map<String, String> data) {
    final StringJoiner sj = new StringJoiner("&");
    for (final Map.Entry<String, String> entry : data.entrySet()) {
      sj.add(URLEncoder.encode(entry.getKey(), UTF_8) + "=" + URLEncoder.encode(entry.getValue(), UTF_8));
    }
    return sj.toString();
  }

  private String signedJWT() {
    final var signedJWT =
        new SignedJWT(new JWSHeader(RS256),
                      new JWTClaimsSet.Builder()
                          .jwtID(randomUUID().toString())
                          .subject(clientId)
                          .issuer(clientId)
                          .claim("grant_type", "client_credentials")
                          .claim("aud", realmUrl)
                          .expirationTime(Date.from(now().plus(1, MINUTES)))
                          .build());
    try {
      signedJWT.sign(new RSASSASigner(assertNonNull(privateKeySupplier.getByAlias(authenticationAlias))));
    } catch (final JOSEException e) {
      sneakyThrow(e);
    }
    return signedJWT.serialize();
  }

  private PrivateKey assertNonNull(final PrivateKey privateKey) {
    if (privateKey == null) {
      throw new IllegalStateException("Alias `" + authenticationAlias + "` is unknown by the given PrivateKeySupplier: " +
                                      "if your PrivateKeySupplier uses a keystore, the alias/entry is probably missing");
    }
    return privateKey;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private String authenticationAlias;
    private String clientId;
    private String fromHeaderValue;
    private PrivateKeySupplier privateKeySupplier;
    private String pseudoUrl;
    // httpapi-int.ehealth.fgov.be/auth/realms/M2M
    private String realmUrl;

    /**
     * Set the alias of the private key to use sign the JWTs sent to eHealth.
     * Only set it if you don't want to use the default value {@code "authentication"}.
     * <p>
     * This alias will be used as parameter of {@link PrivateKeySupplier#getByAlias(String)}.
     * Most of the time, it will be the alias of the {@code "authentication"} entry in the keystore.
     *
     * @param authenticationAlias alias of the private key to use sign the JWTs sent to eHealth
     * @return {@code this}
     */
    public Builder authenticationAlias(final String authenticationAlias) {
      this.authenticationAlias = authenticationAlias;
      return this;
    }

    /**
     * Set the {@code clientId} to use when requesting the JWTs.
     *
     * @param clientId the {@code clientId} to use when requesting the JWTs
     * @return {@code this}
     */
    public Builder clientId(final String clientId) {
      this.clientId = clientId;
      return this;
    }

    /**
     * Set the value of the {@code from} HTTP header to add to all request to eHealth.
     *
     * @param fromHeaderValue the value of the {@code from} HTTP header
     * @return {@code this}
     */
    public Builder fromHeaderValue(final String fromHeaderValue) {
      this.fromHeaderValue = fromHeaderValue;
      return this;
    }

    /**
     * Set the {@link PrivateKeySupplier} to use to retrieve the private key to use to sign the JWTs.
     * <p>
     * The provided {@link PrivateKeySupplier} will be used each time it is required (no caching).
     * It allows you to return a new private key if the keystore has been updated, for example.
     * To prevent performance issues, please cache the private keys in the provided {@link PrivateKeySupplier} and reload it from disk only when needed.
     *
     * @param privateKeySupplier the {@link PrivateKeySupplier} to use to retrieve the private key to use to sign the JWTs
     * @return {@code this}
     */
    public Builder privateKeySupplier(final PrivateKeySupplier privateKeySupplier) {
      this.privateKeySupplier = privateKeySupplier;
      return this;
    }

    /**
     * The URL of the eHealth pseudonymisation service. Only set it if you don't want the default URLs.
     * <p>
     * Default value for {@code dev}, {@code tst}, {@code int} and {@code acc}:
     * <a href="https://api-acpt.ehealth.fgov.be/pseudo/v1">https://api-acpt.ehealth.fgov.be/pseudo/v1</a>.
     * <p>
     * Default value for {@code prd}:
     * <a href="https://api.ehealth.fgov.be/pseudo/v1">https://api.ehealth.fgov.be/pseudo/v1</a>.
     * <p>
     * It uses the prd URL by default when the {@code ENV} environment variable equals {@code prd}, and the acc URL in every other cases.
     *
     * @param pseudoUrl the URL of the eHealth pseudonymisation service
     * @return {@code this}
     */
    public Builder pseudoUrl(final String pseudoUrl) {
      this.pseudoUrl = pseudoUrl;
      return this;
    }

    /**
     * The URL of the realm to use to create the JWTs. Only set it if you don't want the default URLs.
     * <p>
     * Default value for {@code dev}, {@code tst}, {@code int} and {@code acc}:
     * <a href="https://api-acpt.ehealth.fgov.be/auth/realms/M2M">https://api-acpt.ehealth.fgov.be/auth/realms/M2M</a>.
     * <p>
     * Default value for {@code prd}:
     * <a href="https://api.ehealth.fgov.be/auth/realms/M2M">https://api.ehealth.fgov.be/auth/realms/M2M</a>.
     * <p>
     * It uses the prd URL by default when the {@code ENV} environment variable equals {@code prd}, and the acc URL in every other cases.
     *
     * @param realmUrl the URL of the eHealth pseudonymisation service
     * @return {@code this}
     */
    public Builder realmUrl(final String realmUrl) {
      this.realmUrl = realmUrl;
      return this;
    }

    /**
     * Build the {@link StandardPseudonymisationClient}.
     *
     * @return the built {@link StandardPseudonymisationClient}.
     */
    public StandardPseudonymisationClient build() {
      return new StandardPseudonymisationClient(eHealthUrl(pseudoUrl, "/pseudo/v1"),
                                                eHealthUrl(realmUrl, "/auth/realms/M2M"),
                                                clientId, fromHeaderValue, privateKeySupplier,
                                                Optional.ofNullable(authenticationAlias).filter(s -> !s.isBlank()).orElse("authentication"));
    }

    private String eHealthUrl(final String url, final String defaultSuffix) {
      if (url == null || url.isBlank()) {
        return "https://api" + ("prd".equals(System.getenv("ENV")) ? "" : "-acpt") + ".ehealth.fgov.be" + defaultSuffix;
      }
      return url;
    }
  }
}
