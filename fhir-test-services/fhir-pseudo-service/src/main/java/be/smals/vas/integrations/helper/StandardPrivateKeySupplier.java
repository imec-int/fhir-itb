package be.smals.vas.integrations.helper;

import static be.smals.vas.integrations.helper.utils.SneakyThrowUtil.sneakyThrow;
import static be.smals.vas.integrations.helper.utils.ThrowingFunction.sneaky;
import static be.smals.vas.integrations.helper.utils.ThrowingSupplier.sneaky;
import static java.util.stream.Collectors.toMap;

import be.smals.vas.integrations.helper.utils.SupplierWithParameter;
import be.smals.vas.integrations.helper.utils.ThrowingPredicate;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Enumeration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Semaphore;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Supply the PrivateKey matching the no padding Base64 URL encoded SHA-256 hash of a certificate used to encrypt data.
 *
 * <p>This implementation look for the private keys in the keystore provided in the constructor.</p>
 * <p>A lock prevents multiple threads to load the </p>
 */
public class StandardPrivateKeySupplier implements PrivateKeySupplier {

  private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder().withoutPadding();
  private final Supplier<KeyStore> keyStoreSupplier;
  private final SupplierWithParameter<String, KeyStore.ProtectionParameter> entryProtectionParameterSupplier;
  private final Semaphore lock;
  private volatile Map<String, PrivateKey> privateKeysByHash = null;
  private volatile Map<String, PrivateKey> privateKeysByAliases = null;

  /**
   * @param keyStoreSupplier                 Supplier of the keystore containing the private keys to supply.
   * @param entryProtectionParameterSupplier Supplier of the password for the keystore entries. The entry name (alias) is given as parameter of this supplier.
   */
  public StandardPrivateKeySupplier(final Supplier<KeyStore> keyStoreSupplier,
                                    final SupplierWithParameter<String, KeyStore.ProtectionParameter> entryProtectionParameterSupplier) {
    this.keyStoreSupplier = keyStoreSupplier;
    this.entryProtectionParameterSupplier = entryProtectionParameterSupplier;
    lock = new Semaphore(1);
    refresh();
  }

  /**
   * Refresh the private keys.
   *
   * <p>Call this method if you know the keystore has been updated.</p>
   * <p>This method is automatically called by the constructor: there is no need to call it manually to preload the private keys.</p>
   * <p>
   * If the refresh is ongoing on another thread, no refresh is done.
   * If the keystore is not up-to-date, it means that probably a lot of request already failed.
   * It is not a problem is few ones fail too.
   * To immediately skip the refresh prevent blocking all the treads.
   * </p>
   */
  public void refresh() {
    if (lock.tryAcquire()) {
      try {
        final var keyStore = keyStoreSupplier.get();
        privateKeysByHash =
            streamEntries(keyStore)
                .map(Map.Entry::getValue)
                .collect(toMap(sneaky(entry -> base64Encoder.encodeToString(sha256(((PrivateKeyEntry) entry).getCertificate().getEncoded()))),
                               entry -> ((PrivateKeyEntry) entry).getPrivateKey()));
        privateKeysByAliases = streamEntries(keyStore)
                                   .collect(toMap(sneaky(Map.Entry::getKey), entry -> ((PrivateKeyEntry) entry.getValue()).getPrivateKey()));
      } catch (final KeyStoreException e) {
        sneakyThrow(e);
      } finally {
        lock.release();
      }
    }
  }

  @Override
  public PrivateKey getByHash(final String hash) {
    var privateKey = privateKeysByHash.get(hash);
    if (privateKey == null) {
      refresh();
      privateKey = privateKeysByHash.get(hash);
    }
    if (privateKey == null) {
      throw new RuntimeException("No private key found in keystore for certificate with no padding base64url SHA-256 hash `" + hash + "`");
    }
    return privateKey;
  }

  @Override
  public PrivateKey getByAlias(final String alias) {
    return privateKeysByAliases.get(alias);
  }

  private byte[] sha256(final byte[] toDigest) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(toDigest);
    } catch (final NoSuchAlgorithmException e) {
      return sneakyThrow(e);
    }
  }

  private Stream<Map.Entry<String, KeyStore.Entry>> streamEntries(final KeyStore keyStore) throws KeyStoreException {
    final Enumeration<String> aliases = keyStore.aliases();
    return Stream.generate(sneaky(aliases::nextElement)).limit(keyStore.size())
                 .filter(ThrowingPredicate.sneaky(alias -> {
                   final var entry = keyStore.getEntry(alias,
                                                       Optional.ofNullable(entryProtectionParameterSupplier.get(alias))
                                                               .orElseThrow(() -> new RuntimeException("Password for keystore entry `" + alias + "` not found")));
                   return entry instanceof PrivateKeyEntry && ((PrivateKeyEntry) entry).getCertificate() instanceof X509Certificate;
                 }))
                 .collect(Collectors.toMap(alias -> alias,
                                           sneaky(alias -> keyStore.getEntry(alias,
                                                                             Optional.ofNullable(entryProtectionParameterSupplier.get(alias))
                                                                                     .orElseThrow(() -> new RuntimeException("Password for keystore entry `" + alias + "` not found"))))))
                 .entrySet()
                 .stream();
  }
}
