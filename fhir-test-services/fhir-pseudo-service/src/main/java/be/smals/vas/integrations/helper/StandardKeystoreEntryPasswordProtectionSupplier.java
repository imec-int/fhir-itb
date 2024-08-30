package be.smals.vas.integrations.helper;

import be.smals.vas.integrations.helper.utils.SupplierWithParameter;
import java.security.KeyStore;

/**
 * Supply a KeyStore.PasswordProtection based on the password given in the constructor.
 *
 * <p>It assumes all entries in the keystore are protected by this password.</p>
 */
public class StandardKeystoreEntryPasswordProtectionSupplier implements SupplierWithParameter<String, KeyStore.ProtectionParameter> {

  private final KeyStore.PasswordProtection password;

  public StandardKeystoreEntryPasswordProtectionSupplier(final String password) {
    this.password = new KeyStore.PasswordProtection(password.toCharArray());
  }

  @Override
  public KeyStore.ProtectionParameter get(final String ignored) {
    return password;
  }
}
