package be.smals.vas.integrations.helper;

import be.smals.vas.integrations.helper.Pseudonym.PseudonymImpl;
import be.smals.vas.integrations.helper.exception.InvalidPseudonymException;
import be.smals.vas.integrations.helper.internal.PointFactory;
import com.nimbusds.jose.shaded.gson.JsonElement;
import com.nimbusds.jose.shaded.gson.JsonObject;
import java.math.BigInteger;
import java.util.Base64;
import java.util.Optional;
import org.bouncycastle.math.ec.ECPoint;

public interface PseudonymFactory {

  /**
   * @param x BigInteger representation of the X coordinate.
   * @param y BigInteger representation of the Y coordinate.
   * @return Pseudonym
   * @throws InvalidPseudonymException If the coordinates are invalid.
   */
  Pseudonym fromXY(final BigInteger x, final BigInteger y) throws InvalidPseudonymException;

  /**
   * @param x byte[] representation of the X coordinate.
   * @param y byte[] representation of the Y coordinate.
   * @return Point
   * @throws InvalidPseudonymException If the coordinates are invalid.
   */
  Pseudonym fromXY(final byte[] x, final byte[] y) throws InvalidPseudonymException;

  /**
   * @param x             Base64 string representation of the X coordinate.
   * @param y             Base64 string representation of the Y coordinate.
   * @param base64Decoder Base64 decoder
   * @return Pseudonym
   * @throws InvalidPseudonymException If the coordinates or the format are invalid.
   */
  Pseudonym fromXY(final String x, final String y, final Base64.Decoder base64Decoder) throws InvalidPseudonymException;

  /**
   * @param x Base64 string representation of the X coordinate.
   * @param y Base64 string representation of the Y coordinate.
   * @return Pseudonym
   * @throws InvalidPseudonymException If the coordinates or the format are invalid.
   */
  Pseudonym fromXY(final String x, final String y) throws InvalidPseudonymException;

  /**
   * @param asn1 ASN.1 representation of the point (can be ASN.1 compressed format).
   * @return Pseudonym
   * @throws InvalidPseudonymException If the coordinates or the format are invalid.
   */
  Pseudonym fromAsn1(final byte[] asn1) throws InvalidPseudonymException;

  /**
   * @param asn1          Base64 string representation of the ASN.1 encoded point (can be ASN.1 compressed format).
   * @param base64Decoder Base64 decoder
   * @return Pseudonym
   * @throws InvalidPseudonymException If the coordinates or the format are invalid.
   */
  Pseudonym fromAsn1(final String asn1, final Base64.Decoder base64Decoder) throws InvalidPseudonymException;

  /**
   * @param asn1 Base64 string representation of the ASN.1 encoded point (can be ASN.1 compressed format).
   * @return Pseudonym
   * @throws InvalidPseudonymException If the coordinates or the format are invalid.
   */
  Pseudonym fromAsn1(final String asn1) throws InvalidPseudonymException;

  /**
   * Create a pseudonym from X and Y encoded in Base64 and separated by the given separator.
   * <p>
   * The usage of this method is not recommended. Use {@link #fromAsn1(String)} instead.
   *
   * @param pseudonym X and Y encoded in Base64 and separated by the given separator
   * @param separator the character used to separate X and Y
   * @return the parsed pseudonym.
   * @throws InvalidPseudonymException If the coordinates or the format are invalid.
   */
  Pseudonym fromXAndYAsBase64SeparatedBy(final String pseudonym, final char separator) throws InvalidPseudonymException;

  class PseudonymFactoryImpl extends PointFactory implements PseudonymFactory {

    PseudonymFactoryImpl(final Domain domain) {
      super(domain);
    }

    /**
     * @param x BigInteger representation of the X coordinate.
     * @param y BigInteger representation of the Y coordinate.
     * @return Pseudonym
     * @throws InvalidPseudonymException If the coordinates are invalid.
     */
    @Override
    public Pseudonym fromXY(final BigInteger x, final BigInteger y) throws InvalidPseudonymException {
      assertNotNull(x, "The X coordinate is null");
      assertNotNull(y, "The Y coordinate is null");
      return new PseudonymImpl(createEcPoint(x, y), domain);
    }

    /**
     * @param x byte[] representation of the X coordinate.
     * @param y byte[] representation of the Y coordinate.
     * @return Point
     * @throws InvalidPseudonymException If the coordinates are invalid.
     */
    @Override
    public Pseudonym fromXY(final byte[] x, final byte[] y) throws InvalidPseudonymException {
      assertNotNull(x, "The X coordinate is null");
      assertNotNull(y, "The Y coordinate is null");
      return new PseudonymImpl(createEcPoint(toBigInteger(x, "The X coordinate is an invalid byte[]"),
                                             toBigInteger(y, "The Y coordinate is an invalid byte[]")),
                               domain);
    }

    /**
     * @param x             Base64 string representation of the X coordinate.
     * @param y             Base64 string representation of the Y coordinate.
     * @param base64Decoder Base64 decoder
     * @return Pseudonym
     * @throws InvalidPseudonymException If the coordinates or the format are invalid.
     */
    @Override
    public Pseudonym fromXY(final String x, final String y, final Base64.Decoder base64Decoder) throws InvalidPseudonymException {
      assertNotEmpty(x, "The Base64 encoded X coordinate is empty or null");
      assertNotEmpty(y, "The Base64 encoded Y coordinate is empty or null");
      final byte[] decodedX = decodeBase64(x, base64Decoder, "The Base64 encoded X coordinate is not a valid Base64 String");
      final byte[] decodedY = decodeBase64(y, base64Decoder, "The Base64 encoded Y coordinate is not a valid Base64 String");
      return fromXY(toBigInteger(decodedX, "The Base64 encoded X coordinate is not a valid point coordinate"),
                    toBigInteger(decodedY, "The Base64 encoded Y coordinate is not a valid point coordinate"));
    }

    /**
     * @param x Base64 string representation of the X coordinate.
     * @param y Base64 string representation of the Y coordinate.
     * @return Pseudonym
     * @throws InvalidPseudonymException If the coordinates or the format are invalid.
     */
    @Override
    public Pseudonym fromXY(final String x, final String y) throws InvalidPseudonymException {
      return fromXY(x, y, Base64.getDecoder());
    }

    /**
     * @param asn1 ASN.1 representation of the point (can be ASN.1 compressed format).
     * @return Pseudonym
     * @throws InvalidPseudonymException If the coordinates or the format are invalid.
     */
    @Override
    public Pseudonym fromAsn1(final byte[] asn1) throws InvalidPseudonymException {
      assertNotNull(asn1, "The ASN.1 representation of the point is null");
      return new PseudonymImpl(decodeAsn1(asn1), domain);
    }

    /**
     * @param asn1          Base64 string representation of the ASN.1 encoded point (can be ASN.1 compressed format).
     * @param base64Decoder Base64 decoder
     * @return Pseudonym
     * @throws InvalidPseudonymException If the coordinates or the format are invalid.
     */
    @Override
    public Pseudonym fromAsn1(final String asn1, final Base64.Decoder base64Decoder) throws InvalidPseudonymException {
      assertNotNull(asn1, "The Base64 encoded ASN.1 representation of the point is null");
      return fromAsn1(decodeBase64(asn1, base64Decoder, "The Base64 encoded ASN.1 representation of the point is not a valid Base64 String"));
    }

    /**
     * @param asn1 Base64 string representation of the ASN.1 encoded point (can be ASN.1 compressed format).
     * @return Pseudonym
     * @throws InvalidPseudonymException If the coordinates or the format are invalid.
     */
    @Override
    public Pseudonym fromAsn1(final String asn1) throws InvalidPseudonymException {
      return fromAsn1(asn1, Base64.getDecoder());
    }

    /**
     * Create a pseudonym from X and Y encoded in Base64 and separated by the given separator.
     * <p>
     * The usage of this method is not recommended. Use {@link #fromAsn1(String)} instead.
     *
     * @param pseudonym X and Y encoded in Base64 and separated by the given separator
     * @param separator the character used to separate X and Y
     * @return the parsed pseudonym.
     * @throws InvalidPseudonymException If the coordinates or the format are invalid.
     */
    @Override
    public Pseudonym fromXAndYAsBase64SeparatedBy(final String pseudonym, final char separator) throws InvalidPseudonymException {
      if (pseudonym == null || pseudonym.isBlank()) {
        throw new InvalidPseudonymException("Pseudonym is empty");
      }
      final var separatorPos = pseudonym.indexOf(separator);
      if (separatorPos == -1 || pseudonym.indexOf(separator, separatorPos + 1) != -1) {
        throw new InvalidPseudonymException("X and Y are not separated by `" + separator + "`");
      }
      return fromXY(pseudonym.substring(0, separatorPos), pseudonym.substring(separatorPos + 1));
    }

    private ECPoint createEcPoint(final BigInteger x, final BigInteger y) throws InvalidPseudonymException {
      try {
        return domain.curve().createPoint(x, y);
      } catch (final Exception e) {
        throw new InvalidPseudonymException("Invalid coordinates", e);
      }
    }

    private Pseudonym from(final BigInteger x, final BigInteger y) throws InvalidPseudonymException {
      return new PseudonymImpl(createEcPoint(x, y), domain);
    }

    Pseudonym fromResponse(final JsonObject response) {
      final var domainFromResponse = Optional.ofNullable(response.get("domain"))
                                             .map(JsonElement::getAsString)
                                             .orElseThrow(() -> new RuntimeException("Pseudonym sent by eHealth is invalid: `domain` is missing"));
      if (!domainFromResponse.equals(domain.key())) {
        throw new RuntimeException("Pseudonym sent by eHealth is invalid: `" +
                                   domainFromResponse + "` does not match the expected domain `" + domain.key() + "`");
      }
      try {
        return fromXY(response.get("x").getAsString(), response.get("y").getAsString());
      } catch (final InvalidPseudonymException e) {
        throw new RuntimeException("Pseudonym sent by eHealth is invalid", e);
      }
    }

    private static void assertNotNull(final Object o, final String exceptionMessage) throws InvalidPseudonymException {
      if (o == null) {
        throw new InvalidPseudonymException(exceptionMessage);
      }
    }

    private static void assertNotEmpty(final String string, final String exceptionMessage) throws InvalidPseudonymException {
      if (string == null || string.isBlank()) {
        throw new InvalidPseudonymException(exceptionMessage);
      }
    }

    private static BigInteger toBigInteger(final byte[] bytes, final String exceptionMessage) throws InvalidPseudonymException {
      try {
        return new BigInteger(bytes);
      } catch (final Exception e) {
        throw new InvalidPseudonymException(exceptionMessage, e);
      }
    }

    private static byte[] decodeBase64(final String string, final Base64.Decoder base64Decoder, final String exceptionMessage) throws
                                                                                                                               InvalidPseudonymException {
      try {
        return base64Decoder.decode(string);
      } catch (final Exception e) {
        throw new InvalidPseudonymException(exceptionMessage, e);
      }
    }

    private ECPoint decodeAsn1(final byte[] asn1) throws InvalidPseudonymException {
      try {
        return domain.curve().decodePoint(asn1);
      } catch (final Exception e) {
        throw new InvalidPseudonymException("Invalid ASN.1 representation of the point", e);
      }
    }
  }
}
