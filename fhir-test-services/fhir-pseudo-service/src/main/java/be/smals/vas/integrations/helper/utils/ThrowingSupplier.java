package be.smals.vas.integrations.helper.utils;

import static be.smals.vas.integrations.helper.utils.SneakyThrowUtil.sneakyThrow;
import static java.util.Objects.requireNonNull;

import java.util.function.Supplier;

/**
 * Represents a function that accepts zero arguments and returns some value.
 *
 * <p>Function might throw a checked exception instance.</p>
 *
 * @param <T> the type of the output to the function
 * @param <E> the type of the thrown checked exception
 */
@FunctionalInterface
public interface ThrowingSupplier<T, E extends Exception> {
  T get() throws E;

  /**
   * Returns a new Supplier instance which rethrows the checked exception using the Sneaky Throws pattern
   *
   * @return Supplier instance that rethrows the checked exception using the Sneaky Throws pattern
   */
  static <T> Supplier<T> sneaky(final ThrowingSupplier<? extends T, ?> supplier) {
    requireNonNull(supplier);
    return () -> {
      try {
        return supplier.get();
      } catch (final Exception ex) {
        return sneakyThrow(ex);
      }
    };
  }
}