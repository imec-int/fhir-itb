package be.smals.vas.integrations.helper;

/**
 * Implement this interface to call the eHealth Pseudonymisation service.
 */
public interface PseudonymisationClient {

  /**
   * Call to /pseudo/v1/domains/{domainKey} and return the response as a String.
   * <p>
   * Each call to this method <strong>must</strong> make a call to eHealth pseudonymisation service: please do not return a cached response !
   *
   * @param domainKey the domain key
   * @return the response as a String
   */
  String getDomain(String domainKey);

  /**
   * Call to /pseudo/v1/domains/{domainKey}/identify with the given payload and return the response as a String.
   *
   * @param domainKey the domain key
   * @param payload   the request body
   * @return the response as a String
   */
  default String identify(final String domainKey, final String payload) {
    throw new UnsupportedOperationException();
  }

  /**
   * Call to /pseudo/v1/domains/{domainKey}/pseudonymize with the given payload and return the response as a String.
   *
   * @param domainKey the domain key
   * @param payload   the request body
   * @return the response as a String
   */
  default String pseudonymize(final String domainKey, final String payload) {
    throw new UnsupportedOperationException();
  }

  /**
   * Call to /pseudo/v1/domains/{fromDomainKey}/convertTo/{toDomainKey} with the given payload and return the response as a String.
   *
   * @param fromDomainKey the domain of the pseudonym to convert
   * @param toDomainKey   the target domain
   * @param payload       the request body
   * @return the response as a String
   */
  default String convertTo(final String fromDomainKey, final String toDomainKey, final String payload) {
    throw new UnsupportedOperationException();
  }
}
