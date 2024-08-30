package be.smals.vas.integrations.helper;

import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.MINUTES;

import be.smals.vas.integrations.helper.PseudonymInTransitFactory.PseudonymInTransitFactoryImpl;
import be.smals.vas.integrations.helper.TransitInfo.TransitInfoImpl;
import be.smals.vas.integrations.helper.Value.ValueImpl;
import be.smals.vas.integrations.helper.exception.InvalidTransitInfoException;
import be.smals.vas.integrations.helper.exception.UnknownKidException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.factories.DefaultJWEDecrypterFactory;
import com.nimbusds.jose.shaded.gson.JsonObject;
import com.nimbusds.jose.shaded.gson.JsonParser;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;

public sealed interface PseudonymInTransit extends Pseudonym permits PseudonymInTransit.PseudonymInTransitImpl {

  /**
   * Returns the {@link TransitInfo}.
   *
   * @return the {@link TransitInfo}
   */
  TransitInfo transitInfo();

  /**
   * Return the provided {@code iat} (not the {@code iat} from the {@link TransitInfo}).
   *
   * @return the provided {@code iat}
   */
  Long iat();

  /**
   * Return the provided {@code exp} (not the {@code exp} from the {@link TransitInfo}).
   *
   * @return the provided {@code exp}
   */
  Long exp();

  /**
   * Returns the standard String representation of this {@link PseudonymInTransit}.
   * <p>
   * It returns the Base64 URL representation of the compressed ASN.1 representation of the point
   * followed by `:` and by the String representation of the {@link TransitInfo} (JWE compact).
   *
   * @return the standard String representation of this {@link PseudonymInTransit}
   */
  String asString();

  /**
   * Identify (de-pseudonymise) this {@link PseudonymInTransit}.
   *
   * @return the identified value.
   */
  Value identify();

  /**
   * Decrypt the pseudonym in transit.
   *
   * @return The pseudonym at rest.
   */
  PseudonymAtRest decrypt() throws InvalidTransitInfoException;

  /**
   * Decrypt the pseudonym in transit.
   *
   * @param validateIatAndExp must {@code iat} and {@code exp} be validated ?
   * @return The pseudonym at rest.
   */
  PseudonymAtRest decrypt(boolean validateIatAndExp) throws InvalidTransitInfoException;

  PseudonymInTransit convertTo(String toDomainKey);

  final class PseudonymInTransitImpl extends PseudonymImpl implements PseudonymInTransit {

    private static final Base64.Encoder base64UrlEncoderWithoutPadding = Base64.getUrlEncoder().withoutPadding();
    private static final DefaultJWEDecrypterFactory jweDecrypterFactory = new DefaultJWEDecrypterFactory();
    private static final Duration CLOCK_SKEW = Duration.of(1, MINUTES);  // as per ehealth spec

    private final TransitInfoImpl transitInfo;
    private final Long iat;
    private final Long exp;

    PseudonymInTransitImpl(final Pseudonym pseudonym, final TransitInfo transitInfo, final Long iat, final Long exp) {
      super(((PseudonymImpl) pseudonym).ecPoint, ((PseudonymImpl) pseudonym).domain);
      this.transitInfo = (TransitInfoImpl) transitInfo;
      this.iat = iat;
      this.exp = exp;
    }

    PseudonymInTransitImpl(final Pseudonym pseudonym, final TransitInfo transitInfo) {
      this(pseudonym, transitInfo, null, null);
    }

    @Override
    public TransitInfo transitInfo() {
      return transitInfo;
    }

    @Override
    public Long iat() {
      return iat;
    }

    @Override
    public Long exp() {
      return exp;
    }

    @Override
    public String asString() {
      return asn1Base64Compressed(base64UrlEncoderWithoutPadding) + ":" + transitInfo.asString();
    }

    @Override
    public Value identify() {
      final var random = domain.createRandom();
      final var blindedPseudonym = multiply(random);
      final var payload = domain.createPayload(blindedPseudonym, transitInfo().asString());
      final var rawResponse = domain.pseudonymisationClient().identify(domain.key(), payload);
      final var response = (JsonObject) JsonParser.parseString(rawResponse);
      final var blindedValue = (PseudonymImpl) ((PseudonymFactory.PseudonymFactoryImpl) domain.pseudonymFactory()).fromResponse(response);
      return new ValueImpl(blindedValue.multiplyByModInverse(random).ecPoint, domain);
    }

    @SuppressWarnings("DuplicateThrows")
    @Override
    public PseudonymAtRest decrypt() throws InvalidTransitInfoException, UnknownKidException {
      return decrypt(true);
    }

    @SuppressWarnings("DuplicateThrows")
    @Override
    public PseudonymAtRest decrypt(final boolean validateIatAndExp) throws InvalidTransitInfoException, UnknownKidException {
      final var decryptedTransitInfo = decryptTransitInfo();
      if (validateIatAndExp) {
        validateDecryptedTransitInfo(decryptedTransitInfo);
      }
      final var scalar = new BigInteger(Base64.getDecoder().decode((String) decryptedTransitInfo.get("scalar")));
      return new PseudonymAtRest.PseudonymAtRestImpl(ecPoint.multiply(scalar).normalize(), domain);
    }

    @Override
    public PseudonymInTransit convertTo(final String toDomainKey) {
      final var random = domain.createRandom();
      final var blindedPseudonym = multiply(random);
      final var payload = domain.createPayload(blindedPseudonym, transitInfo.asString());
      final var rawResponse = domain.pseudonymisationClient().convertTo(domain.key(), toDomainKey, payload);
      final var targetDomain = domain.pseudonymisationHelper().getDomain(toDomainKey);
      return ((PseudonymInTransitFactoryImpl) targetDomain.pseudonymInTransitFactory()).fromRawResponse(rawResponse, random);
    }

    /**
     * Decrypt the transitInfo in JWECompact format.
     *
     * @return Json Map of transitInfo decrypted
     */
    @SuppressWarnings("DuplicateThrows")
    private Map<String, Object> decryptTransitInfo() throws InvalidTransitInfoException, UnknownKidException {
      try {
        transitInfo.validateHeader();
        final var parsedTransitInfo = transitInfo.parse();
        final var transitInfoHeader = parsedTransitInfo.getHeader();
        final var secretKey = domain.getSecretKey(transitInfoHeader.getKeyID());
        if (secretKey == null) {
          throw new UnknownKidException(transitInfoHeader.getKeyID());
        }
        parsedTransitInfo.decrypt(jweDecrypterFactory.createJWEDecrypter(transitInfoHeader, secretKey));
        return parsedTransitInfo.getPayload().toJSONObject();
      } catch (final JOSEException e) {
        throw new InvalidTransitInfoException("Error when decrypting transitInfo", e);
      }
    }

    private void validateDecryptedTransitInfo(final Map<String, Object> transitInfo) throws InvalidTransitInfoException {
      final long iat = (long) transitInfo.get("iat");
      final long exp = (long) transitInfo.get("exp");
      final var currentTime = now();
      if (Instant.ofEpochSecond(iat).isAfter(currentTime.plus(CLOCK_SKEW))) {
        throw new InvalidTransitInfoException("transitInfo not yet ready for use (iat > now)");
      }
      if (Instant.ofEpochSecond(exp).isBefore(currentTime.minus(CLOCK_SKEW))) {
        throw new InvalidTransitInfoException("expired transitInfo (exp < now)");
      }
    }

    @Override
    public boolean equals(final Object obj) {
      if (obj == this) {return true;}
      if (obj == null || obj.getClass() != this.getClass()) {return false;}
      final var that = (PseudonymInTransitImpl) obj;
      return super.equals(that) &&
             Objects.equals(this.transitInfo, that.transitInfo) &&
             Objects.equals(this.iat, that.iat) &&
             Objects.equals(this.exp, that.exp);
    }

    @Override
    public int hashCode() {
      return Objects.hash(super.hashCode(), transitInfo, iat, exp);
    }

    @Override
    public String toString() {
      return "{" +
             "\"x\": \"" + ecPoint.getXCoord().toBigInteger() + "\", " +
             "\"y\": \"" + ecPoint.getYCoord().toBigInteger() + "\"," +
             "\"domain\": \"" + domain.key() + "\"," +
             "\"transitInfo\": " + transitInfo + ", " +
             "\"iat\": " + OffsetDateTime.ofInstant(Instant.ofEpochSecond(iat), ZoneId.systemDefault()) + ", " +
             "\"exp\": " + OffsetDateTime.ofInstant(Instant.ofEpochSecond(exp), ZoneId.systemDefault()) + '}';
    }
  }
}
