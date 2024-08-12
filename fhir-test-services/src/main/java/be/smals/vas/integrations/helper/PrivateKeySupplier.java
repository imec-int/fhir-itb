package be.smals.vas.integrations.helper;

import java.security.PrivateKey;

/**
 * Provides the private keys to use to decrypt the secret keys of the domain or to sign the JWTs.
 */
public interface PrivateKeySupplier {

  /**
   * Returns the private key by its no padding Base64 URL SHA-256 hash.
   *
   * @param hash the no padding Base64 URL SHA-256 hash of the private key to return
   * @return The private key matching the given hash or {@code null} of there is no matching private key.
   */
  PrivateKey getByHash(String hash);

  /**
   * Returns the private key by its alias.
   * <p>
   * If the private keys are in a keystore, it is the alias of the entry.
   * If the private keys is somewhere else (in a database, for example), it is the alias of the private key in this system (in the database in our example).
   *
   * @param alias the alias of the private key
   * @return the private key matching the given alias.
   */
  PrivateKey getByAlias(String alias);
}
