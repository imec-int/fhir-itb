package be.smals.vas.integrations.helper;

import static be.smals.vas.integrations.helper.utils.SneakyThrowUtil.sneakyThrow;

import java.io.File;
import java.security.KeyStore;
import java.util.function.Supplier;

/**
 * Standard {@link KeyStore} {@link Supplier} that load the content of the keystore each time {@link #get()} is called.
 */
public class StandardKeystoreSupplier implements Supplier<KeyStore> {

  private final File file;
  private final char[] password;

  public StandardKeystoreSupplier(final File file, final String password) {
    this.file = file;
    this.password = password.toCharArray();
  }

  @Override
  public KeyStore get() {
    try {
      return KeyStore.getInstance(file, password);
    } catch (final Exception e) {
      return sneakyThrow(e);
    }
  }
}
