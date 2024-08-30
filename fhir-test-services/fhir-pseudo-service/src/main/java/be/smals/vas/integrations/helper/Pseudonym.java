package be.smals.vas.integrations.helper;

import be.smals.vas.integrations.helper.Point.PointImpl;
import be.smals.vas.integrations.helper.PseudonymAtRest.PseudonymAtRestImpl;
import be.smals.vas.integrations.helper.PseudonymInTransit.PseudonymInTransitImpl;
import java.math.BigInteger;
import java.util.Base64;
import java.util.Base64.Encoder;
import org.bouncycastle.math.ec.ECPoint;

public sealed interface Pseudonym permits PseudonymAtRest, Pseudonym.PseudonymImpl, PseudonymInTransit {

  /**
   * Returns X coordinate as {@link BigInteger}.
   *
   * @return X coordinate as {@link BigInteger}
   */
  BigInteger x();

  /**
   * Returns Y coordinate as {@link BigInteger}.
   *
   * @return Y coordinate as {@link BigInteger}
   */
  BigInteger y();

  /**
   * Returns binary representation of the X coordinate (as a byte array).
   *
   * @return binary representation of the X coordinate (as a byte array)
   */
  byte[] xAsBytes();

  /**
   * Returns binary representation of the Y coordinate (as a byte array).
   *
   * @return binary representation of the Y coordinate (as a byte array)
   */
  byte[] yAsBytes();

  /**
   * Returns binary representation of the X coordinate (as a byte array converted in a Base64 String using the provided {@link Encoder}).
   *
   * @param base64Encoder the {@link Encoder} to use to convert the byte array in the Base64 String
   * @return binary representation of the X coordinate (as a byte array converted in a Base64 String)
   */
  String xAsBase64(final Encoder base64Encoder);

  /**
   * Returns binary representation of the X coordinate (as a byte array converted in a Base64 String using {@link Base64#getEncoder()}).
   *
   * @return binary representation of the X coordinate (as a byte array converted in a Base64 String)
   */
  String xAsBase64();

  /**
   * Returns binary representation of the Y coordinate (as a byte array converted in a Base64 String using the provided {@link Encoder}).
   *
   * @param base64Encoder the {@link Encoder} to use to convert the byte array in the Base64 String
   * @return binary representation of the Y coordinate (as a byte array converted in a Base64 String)
   */
  String yAsBase64(final Encoder base64Encoder);

  /**
   * Returns binary representation of the Y coordinate (as a byte array converted in a Base64 String using {@link Base64#getEncoder()}).
   *
   * @return binary representation of the Y coordinate (as a byte array converted in a Base64 String)
   */
  String yAsBase64();

  /**
   * Returns X and Y (as a byte arrays converted in Base64 Strings using {@link Base64#getEncoder()}) separated by the given {@code separator}.
   *
   * @param separator the String tu use to separate X and Y
   * @return Returns X and Y (as a byte arrays converted in Base64 Strings)
   */
  String xAndYAsBase64SeparatedBy(final String separator);

  /**
   * Uncompressed ASN.1 representation of this point.
   *
   * @return uncompressed ASN.1 representation of this point
   */
  byte[] asn1();

  /**
   * Uncompressed ASN.1 representation of this point as a Base64 String (using the provided {@link Encoder}.
   *
   * @param base64Encoder the {@link Encoder} to use to encode the byte array to the Base64 String.
   * @return uncompressed ASN.1 representation of this point as a Base64 String
   */
  String asn1Base64(final Encoder base64Encoder);

  /**
   * Uncompressed ASN.1 representation of this point as a Base64 String (using the {@link Base64#getEncoder()}.
   *
   * @return uncompressed ASN.1 representation of this point as a Base64 String
   */
  String asn1Base64();

  /**
   * Compressed ASN.1 representation of this point.
   *
   * @return compressed ASN.1 representation of this point
   */
  byte[] asn1Compressed();

  /**
   * Compressed ASN.1 representation of this point as a Base64 String (using the provided {@link Encoder}.
   *
   * @param base64Encoder the {@link Encoder} to use to encode the byte array to the Base64 String.
   * @return compressed ASN.1 representation of this point as a Base64 String
   */
  String asn1Base64Compressed(final Encoder base64Encoder);

  /**
   * Compressed ASN.1 representation of this point as a Base64 String (using the {@link Base64#getEncoder()}.
   *
   * @return compressed ASN.1 representation of this point as a Base64 String
   */
  String asn1Base64Compressed();

  sealed class PseudonymImpl extends PointImpl implements Pseudonym permits PseudonymAtRestImpl, PseudonymInTransitImpl {

    PseudonymImpl(final ECPoint ecPoint, final Domain domain) {
      super(ecPoint, domain);
    }

    @Override
    public BigInteger x() {
      return ecPoint.getXCoord().toBigInteger();
    }

    @Override
    public BigInteger y() {
      return ecPoint.getYCoord().toBigInteger();
    }

    @Override
    public byte[] xAsBytes() {
      return ecPoint.getXCoord().getEncoded();
    }

    @Override
    public byte[] yAsBytes() {
      return ecPoint.getYCoord().getEncoded();
    }

    @Override
    public String xAsBase64(final Encoder base64Encoder) {
      return base64Encoder.encodeToString(xAsBytes());
    }

    @Override
    public String xAsBase64() {
      return xAsBase64(Base64.getEncoder());
    }

    @Override
    public String yAsBase64(final Encoder base64Encoder) {
      return base64Encoder.encodeToString(yAsBytes());
    }

    @Override
    public String yAsBase64() {
      return yAsBase64(Base64.getEncoder());
    }

    @Override
    public String xAndYAsBase64SeparatedBy(final String separator) {
      return xAsBase64() + separator + yAsBase64();
    }

    @Override
    public byte[] asn1() {
      return ecPoint.getEncoded(false);
    }

    @Override
    public String asn1Base64(final Encoder base64Encoder) {
      return base64Encoder.encodeToString(asn1());
    }

    @Override
    public String asn1Base64() {
      return asn1Base64(Base64.getEncoder());
    }

    @Override
    public byte[] asn1Compressed() {
      return ecPoint.getEncoded(true);
    }

    @Override
    public String asn1Base64Compressed(final Encoder base64Encoder) {
      return base64Encoder.encodeToString(asn1Compressed());
    }

    @Override
    public String asn1Base64Compressed() {
      return asn1Base64Compressed(Base64.getEncoder());
    }

    PseudonymImpl multiply(final BigInteger scalar) {
      return new PseudonymImpl(ecPoint.multiply(scalar).normalize(), domain);
    }

    PseudonymImpl multiplyByModInverse(final BigInteger scalar) {
      return multiply(scalar.modInverse(ecPoint.getCurve().getOrder()));
    }
  }
}
