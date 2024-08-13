package be.smals.vas.integrations.helper;

import be.smals.vas.integrations.helper.exception.InvalidTransitInfoException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import java.text.ParseException;
import java.util.Objects;

public sealed interface TransitInfo permits TransitInfo.TransitInfoImpl {

  /**
   * Returns the JWE compact representation if this {@link TransitInfo}.
   *
   * @return the JWE compact representation if this {@link TransitInfo}.
   */
  String asString();

  /**
   * Returns the audience of this {@link TransitInfo} (JWE).
   *
   * @return the audience of this {@link TransitInfo}
   * @throws InvalidTransitInfoException if the transit info String cannot be parsed or is invalid
   */
  String audience() throws InvalidTransitInfoException;

  /**
   * Validate the header of this {@link TransitInfo} (JWE).
   *
   * @throws InvalidTransitInfoException if the transit info String cannot be parsed or is invalid
   */
  void validateHeader() throws InvalidTransitInfoException;

  final class TransitInfoImpl implements TransitInfo {

    private final Domain.DomainImpl domain;
    private String raw;

    private JWEObject parsed;

    TransitInfoImpl(final Domain domain, final String raw) {
      this.domain = (Domain.DomainImpl) domain;
      this.raw = raw;
    }

    TransitInfoImpl(final Domain domain, final JWEObject jweObject) {
      this.domain = (Domain.DomainImpl) domain;
      this.parsed = jweObject;
    }

    @Override
    public String asString() {
      if (raw == null) {
        raw = parsed.serialize();
      }
      return raw;
    }

    @Override
    public String audience() throws InvalidTransitInfoException {
      return (String) parse().getHeader().getCustomParam("aud");
    }

    JWEObject parse() throws InvalidTransitInfoException {
      if (parsed == null) {
        try {
          parsed = JWEObject.parse(raw);
        } catch (final ParseException e) {
          throw new InvalidTransitInfoException("Error when parsing transitInfo", e);
        }
      }
      final var header = parsed.getHeader();
      if (header.getAlgorithm() != JWEAlgorithm.DIR) {
        throw new InvalidTransitInfoException("`alg` with value `dir` expected in header");
      }
      if (header.getEncryptionMethod() == null) {
        throw new InvalidTransitInfoException("Missing `enc` in header");
      }
      if (Objects.toString(header.getCustomParam("aud"), "").isBlank()) {
        throw new InvalidTransitInfoException("Missing `aud` in header");
      }
      return parsed;
    }

    @Override
    public void validateHeader() throws InvalidTransitInfoException {
      validateTransitInfoHeader(parse().getHeader());
    }

    void validateTransitInfoHeader(final JWEHeader transitInfoHeader) throws InvalidTransitInfoException {
      if (!transitInfoHeader.getCustomParam("aud").equals(domain.audience())) {
        throw new InvalidTransitInfoException("Invalid `aud`");
      }
    }

    @Override
    public boolean equals(final Object obj) {
      if (obj == this) {return true;}
      if (!(obj instanceof final TransitInfoImpl that)) {return false;}
      return Objects.equals(this.raw, that.raw);
    }

    @Override
    public int hashCode() {
      return Objects.hash(raw, parsed);
    }

    @Override
    public String toString() {
      return asString();
    }
  }
}
