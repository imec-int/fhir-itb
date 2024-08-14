package be.smals.vas.integrations.helper;

import static java.math.BigInteger.ONE;
import static java.nio.charset.StandardCharsets.UTF_8;

import be.smals.vas.integrations.helper.Value.ValueImpl;
import be.smals.vas.integrations.helper.exception.InvalidValueException;
import be.smals.vas.integrations.helper.internal.PointFactory;
import java.math.BigInteger;
import java.nio.charset.Charset;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECFieldElement;
import org.bouncycastle.math.ec.ECPoint;

public interface ValueFactory {

  /**
   * Returns the maximum size of the value (as bytes) that can be converted in a Point.
   *
   * @return the maximum size of the value.
   */
  int getMaxValueSize();

  /**
   * @param value Raw value to convert to Value.
   * @return Value
   * @throws InvalidValueException If the value cannot be converted to a Value (if the value is too long).
   */
  Value from(byte[] value) throws InvalidValueException;

  /**
   * @param value Raw value to convert to Value. The bytes will be obtained by getting the bytes of the value for the given charset.
   * @return Value
   * @throws InvalidValueException If the value cannot be converted to a Value (if the value is too long).
   */
  Value from(final String value, final Charset charset) throws InvalidValueException;

  /**
   * @param value Raw value to convert to Value. The bytes will be obtained by getting the UTF-8 bytes of the value.
   * @return Value
   * @throws InvalidValueException If the value cannot be converted to a Value (if the value is too long).
   */
  Value from(final String value) throws InvalidValueException;

  class ValueFactoryImpl extends PointFactory implements ValueFactory {

    private final ECFieldElement a;
    private final ECFieldElement b;

    private final int maxValueSize;

    public ValueFactoryImpl(final Domain domain) {
      super(domain);
      final var curve = this.domain.curve();
      a = curve.getA();
      b = curve.getB();
      // maxValueSize = max size for curve - buffer size - 1 byte to store the value length
      maxValueSize = (curve.getFieldSize() / 8) - this.domain.bufferSize() - 1;
    }

    /**
     * Returns the maximum size of the value (as bytes) that can be converted in a Point.
     *
     * @return the maximum size of the value.
     */
    @Override
    public int getMaxValueSize() {
      return maxValueSize;
    }

    /**
     * @param value Raw value to convert to Value.
     * @return Value
     * @throws InvalidValueException If the value cannot be converted to a Value (if the value is too long).
     */
    @Override
    public Value from(byte[] value) throws InvalidValueException {

      final var i = domain.bufferSize();

      // Process null value as an empty value
      if (value == null) {
        value = new byte[0];
      } else {
        if (value.length > maxValueSize) {
          throw new InvalidValueException("The value is too long: should be max " + maxValueSize + " bytes");
        }
      }

      // Create a new Byte Array with a length 1 + value.length + 1 + bufferSize
      // The first Byte is set to 0
      final var xBytes = new byte[1 + value.length + 1 + i];

      // Copy the value into xBytes starting at position one
      int position = 1;
      System.arraycopy(value, 0, xBytes, position, value.length);
      position += value.length;
      xBytes[position] = (byte) value.length;

      // Compute the X coordinates by converting the xBytes to a BigInteger
      // Then put the X Coordinate on the elliptic curve
      var xCoordinates = new BigInteger(xBytes);

      // Compute y on the elliptic curve
      var y = computeY(xCoordinates);
      while (y == null) {
        xCoordinates = xCoordinates.add(ONE);
        y = computeY(xCoordinates);
      }

      return new ValueImpl(createEcPoint(xCoordinates, y), domain);
    }

    /**
     * @param value Raw value to convert to Value. The bytes will be obtained by getting the bytes of the value for the given charset.
     * @return Value
     * @throws InvalidValueException If the value cannot be converted to a Value (if the value is too long).
     */
    @Override
    public Value from(final String value, final Charset charset) throws InvalidValueException {
      return from(value == null ? null : value.getBytes(charset));
    }

    /**
     * @param value Raw value to convert to Value. The bytes will be obtained by getting the UTF-8 bytes of the value.
     * @return Value
     * @throws InvalidValueException If the value cannot be converted to a Value (if the value is too long).
     */
    @Override
    public Value from(final String value) throws InvalidValueException {
      return from(value, UTF_8);
    }

    private ECPoint createEcPoint(final BigInteger x, final BigInteger y) throws InvalidValueException {
      try {
        return domain.curve().createPoint(x, y);
      } catch (final Exception e) {
        throw new InvalidValueException("Invalid coordinates", e);
      }
    }

    /**
     * Based on the code of Bouncy Castle.
     * <p>
     * The code has been copied because the Bouncy Castle method is not public.
     *
     * @param x the X coordinate as BigInteger
     * @return Point
     * @see ECCurve.AbstractFp#decompressPoint(int, BigInteger)
     */
    @SuppressWarnings("JavadocReference")
    private BigInteger computeY(final BigInteger x) {
      final ECFieldElement xFieldElement = domain.curve().fromBigInteger(x);
      final ECFieldElement rhs = xFieldElement.square().add(a).multiply(xFieldElement).add(b);
      ECFieldElement y = rhs.sqrt();
      return y == null ? null : y.toBigInteger();
    }
  }
}
