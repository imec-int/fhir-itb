package be.smals.vas.integrations.helper;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.nio.charset.Charset;
import java.util.Arrays;
import org.bouncycastle.math.ec.ECPoint;

public sealed interface Value permits Value.ValueImpl {

  /**
   * Returns the value as a bytes array.
   * <p>
   * Use it for non-text values.
   *
   * @return the value as a bytes array
   */
  byte[] asBytes();

  /**
   * Returns the value as a String.
   * <p>
   * Convenient method that converts the bytes array to a String.
   * <p>
   * Use it for text values.
   *
   * @param charset The charset to use to convert the internal bytes array to a String.
   * @return the value as a String
   */
  String asString(final Charset charset);

  /**
   * Returns the value as a String.
   * <p>
   * Convenient method that converts the bytes array (representing UTF-8 characters) to a String.
   * <p>
   * Use it for text values.
   *
   * @return the value as a String
   */
  String asString();

  /**
   * Pseudonymize this {@link Value}.
   *
   * @return a random {@link PseudonymInTransit} for this {@link Value}.
   */
  PseudonymInTransit pseudonymize();

  final class ValueImpl extends Point.PointImpl implements Value {

    ValueImpl(final ECPoint ecPoint, final Domain domain) {
      super(ecPoint, domain);
    }

    @Override
    public byte[] asBytes() {
      final var x = ecPoint.getXCoord().getEncoded();
      final var valueLengthPos = x.length - domain.bufferSize() - 1;
      final var valueLength = x[valueLengthPos];
      final var startPosition = valueLengthPos - valueLength;
      return Arrays.copyOfRange(x, startPosition, startPosition + valueLength);
    }

    @Override
    public String asString(final Charset charset) {
      final var x = ecPoint.getXCoord().getEncoded();
      final var valueLengthPos = x.length - domain.bufferSize() - 1;
      final var valueLength = x[valueLengthPos];
      final var startPosition = valueLengthPos - valueLength;
      return new String(x, startPosition, valueLength, charset);
    }

    @Override
    public String asString() {
      return asString(UTF_8);
    }

    @Override
    public PseudonymInTransit pseudonymize() {
      final var random = domain.createRandom();
      final var blindedValue = new Pseudonym.PseudonymImpl(ecPoint.multiply(random).normalize(), domain);
      final var payload = domain.createPayload(blindedValue);
      final var rawResponse = domain.pseudonymisationClient().pseudonymize(domain.key(), payload);
      return ((PseudonymInTransitFactory.PseudonymInTransitFactoryImpl) domain.pseudonymInTransitFactory()).fromRawResponse(rawResponse, random);
    }

    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder("Value[");
      String separator = "";
      for (final byte aByte : asBytes()) {
        sb.append(separator).append(aByte);
        separator = ",";
      }
      sb.append("]");
      return sb.toString();
    }
  }
}
