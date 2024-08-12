package be.smals.vas.integrations.helper;

import be.smals.vas.integrations.helper.PseudonymAtRest.PseudonymAtRestImpl;
import be.smals.vas.integrations.helper.exception.InvalidPseudonymException;
import be.smals.vas.integrations.helper.internal.PointFactory;
import java.util.Base64;
import org.bouncycastle.math.ec.ECPoint;

public interface PseudonymAtRestFactory {

  /**
   * @param asn1 ASN.1 representation of the point (can be ASN.1 compressed format).
   * @return Pseudonym
   * @throws InvalidPseudonymException If the coordinates or the format are invalid.
   */
  PseudonymAtRest fromAsn1(final byte[] asn1) throws InvalidPseudonymException;

  /**
   * @param asn1          Base64 string representation of the ASN.1 encoded point (can be ASN.1 compressed format).
   * @param base64Decoder Base64 decoder
   * @return Pseudonym
   * @throws InvalidPseudonymException If the coordinates or the format are invalid.
   */
  PseudonymAtRest fromAsn1(final String asn1, final Base64.Decoder base64Decoder) throws InvalidPseudonymException;

  /**
   * @param asn1 Base64 string representation of the ASN.1 encoded point (can be ASN.1 compressed format).
   * @return Pseudonym
   * @throws InvalidPseudonymException If the coordinates or the format are invalid.
   */
  PseudonymAtRest fromAsn1(final String asn1) throws InvalidPseudonymException;

  class PseudonymAtRestFactoryImpl extends PointFactory implements PseudonymAtRestFactory {

    PseudonymAtRestFactoryImpl(final Domain domain) {
      super(domain);
    }

    /**
     * @param asn1 ASN.1 representation of the point (can be ASN.1 compressed format).
     * @return Pseudonym
     * @throws InvalidPseudonymException If the coordinates or the format are invalid.
     */
    @Override
    public PseudonymAtRest fromAsn1(final byte[] asn1) throws InvalidPseudonymException {
      assertNotNull(asn1, "The ASN.1 representation of the point is null");
      return new PseudonymAtRestImpl(decodeAsn1(asn1), domain);
    }

    /**
     * @param asn1          Base64 string representation of the ASN.1 encoded point (can be ASN.1 compressed format).
     * @param base64Decoder Base64 decoder
     * @return Pseudonym
     * @throws InvalidPseudonymException If the coordinates or the format are invalid.
     */
    @Override
    public PseudonymAtRest fromAsn1(final String asn1, final Base64.Decoder base64Decoder) throws InvalidPseudonymException {
      assertNotNull(asn1, "The Base64 encoded ASN.1 representation of the point is null");
      return fromAsn1(decodeBase64(asn1, base64Decoder));
    }

    /**
     * @param asn1 Base64 string representation of the ASN.1 encoded point (can be ASN.1 compressed format).
     * @return Pseudonym
     * @throws InvalidPseudonymException If the coordinates or the format are invalid.
     */
    @Override
    public PseudonymAtRest fromAsn1(final String asn1) throws InvalidPseudonymException {
      return fromAsn1(asn1, Base64.getDecoder());
    }

    private ECPoint decodeAsn1(final byte[] asn1) throws InvalidPseudonymException {
      try {
        return domain.curve().decodePoint(asn1);
      } catch (final Exception e) {
        throw new InvalidPseudonymException("Invalid ASN.1 representation of the point", e);
      }
    }

    private static void assertNotNull(final Object o, final String exceptionMessage) throws InvalidPseudonymException {
      if (o == null) {
        throw new InvalidPseudonymException(exceptionMessage);
      }
    }

    private static byte[] decodeBase64(final String string, final Base64.Decoder base64Decoder) throws InvalidPseudonymException {
      try {
        return base64Decoder.decode(string);
      } catch (final Exception e) {
        throw new InvalidPseudonymException("The Base64 encoded ASN.1 representation of the point is not a valid Base64 String", e);
      }
    }
  }
}
