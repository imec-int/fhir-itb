package be.smals.vas.integrations.helper;

import be.smals.vas.integrations.helper.PseudonymInTransit.PseudonymInTransitImpl;
import be.smals.vas.integrations.helper.TransitInfo.TransitInfoImpl;
import be.smals.vas.integrations.helper.exception.InvalidPseudonymException;
import be.smals.vas.integrations.helper.internal.PointFactory;
import com.nimbusds.jose.shaded.gson.JsonObject;
import com.nimbusds.jose.shaded.gson.JsonParser;
import java.math.BigInteger;
import java.util.Base64;

public interface PseudonymInTransitFactory {

  /**
   * @param pseudonym the {@link Pseudonym} part of the {@link PseudonymInTransit}
   * @param transitInfo the {@link TransitInfo} part ot the {@link PseudonymInTransit}
   * @return A {@link PseudonymInTransit} created from the given {@link Pseudonym} and {@link TransitInfo}
   */
  PseudonymInTransit from(final Pseudonym pseudonym, final TransitInfo transitInfo);

  /**
   * @param asn1AndTransitInfo Base64 URL string representation (without padding) of the ASN.1 encoded point (can be ASN.1 compressed or uncompressed format),
   *                           followed by {@code :}, and by the string representation of the {@link TransitInfo} (JWE compact).
   * @return A {@link PseudonymInTransit} created from the given {@code asn1AndTransitInfo}
   * @throws InvalidPseudonymException if the format of the given {@code asn1AndTransitInfo} is invalid
   */
  PseudonymInTransit fromAsn1AndTransitInfo(final String asn1AndTransitInfo) throws InvalidPseudonymException;

  class PseudonymInTransitFactoryImpl extends PointFactory implements PseudonymInTransitFactory {

    PseudonymInTransitFactoryImpl(final Domain domain) {
      super(domain);
    }

    @Override
    public PseudonymInTransit from(final Pseudonym pseudonym, final TransitInfo transitInfo) {
      return new PseudonymInTransitImpl(pseudonym, transitInfo);
    }

    @Override
    public PseudonymInTransit fromAsn1AndTransitInfo(final String asn1AndTransitInfo) throws InvalidPseudonymException {
      assertNotEmpty(asn1AndTransitInfo);
      int start = 0;
      final var chars = asn1AndTransitInfo.toCharArray();
      final var length = chars.length;
      for (int end = 0; end < length; end++) {
        if (chars[end] == ':') {
          final var pseudonym = domain.pseudonymFactory().fromAsn1(new String(chars, start, end - start), Base64.getUrlDecoder());
          start = end + 1;
          final var transitInfo = new TransitInfoImpl(domain, new String(chars, start, length - start));
          return new PseudonymInTransitImpl(pseudonym, transitInfo);
        }
      }
      throw new InvalidPseudonymException("Missing `:` in the pseudonym in transit string. Format must be {asn1InBase64Url}:{transitInfo}");
    }

    PseudonymInTransit fromRawResponse(final String rawResponse, final BigInteger scalar) {
      final var response = (JsonObject) JsonParser.parseString(rawResponse);
      final var blindedValue = (Pseudonym.PseudonymImpl) ((PseudonymFactory.PseudonymFactoryImpl) domain.pseudonymFactory()).fromResponse(response);
      final var pseudonym = blindedValue.multiplyByModInverse(scalar);
      return new PseudonymInTransitImpl(pseudonym,
                                        new TransitInfoImpl(domain, response.get("transitInfo").getAsString()),
                                        response.get("iat").getAsLong(),
                                        response.get("exp").getAsLong());
    }

    private static void assertNotEmpty(final String string) throws InvalidPseudonymException {
      if (string == null || string.isBlank()) {
        throw new InvalidPseudonymException("The pseudonym in transit string is empty or null");
      }
    }
  }
}
