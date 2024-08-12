package be.smals.vas.integrations.helper;

import java.net.URI;

/**
 * Implement this interface to call the eHealth Pseudonymisation service.
 */
public interface JwksClient {

  /**
   * Return the JWK set as a String.
   * <p>
   * Each call to this method <strong>must</strong> make a call return the current content of the JWK set URL: please do not return a cached response !
   *
   * @return the JWK set as a String
   */
  String getJwks(final URI jwksUrl);
}
