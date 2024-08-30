package be.smals.vas.integrations.helper;

import be.smals.vas.integrations.helper.TransitInfo.TransitInfoImpl;

public interface TransitInfoFactory {

  /**
   * Create a {@link TransitInfo} from the given JWE compact.
   *
   * @param jweCompact JWE compact to parse
   * @return a {@link TransitInfo} created from the given JWE compact
   */
  TransitInfo from(final String jweCompact);

  class TransitInfoFactoryImpl implements TransitInfoFactory {

    private final Domain domain;

    TransitInfoFactoryImpl(final Domain domain) {
      this.domain = domain;
    }

    @Override
    public TransitInfo from(final String jweCompact) {
      return new TransitInfoImpl(domain, jweCompact);
    }
  }
}
