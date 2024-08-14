package be.smals.vas.integrations.helper.utils;

import static be.smals.vas.integrations.helper.utils.SneakyThrowUtil.sneakyThrow;
import static java.util.Objects.requireNonNull;

import java.util.function.Predicate;

/**
 * Represents a predicate (boolean-valued function) of one argument.
 * <p>
 * The predicate might throw a checked exception instance.
 *
 * @param <T> the type of the output to the function
 * @param <E> the type of the thrown checked exception
 * @see Predicate
 */
@FunctionalInterface
public interface ThrowingPredicate<T, E extends Exception> {
  boolean test(T t) throws E;

  /**
   * Returns a new {@link Predicate} instance which rethrows the checked exception using the Sneaky Throws pattern.
   *
   * @return {@link Predicate} instance that rethrows the checked exception using the Sneaky Throws pattern
   */
  static <T> Predicate<T> sneaky(final ThrowingPredicate<T, ?> predicate) {
    requireNonNull(predicate);
    return t -> {
      try {
        return predicate.test(t);
      } catch (final Exception ex) {
        return sneakyThrow(ex);
      }
    };
  }
}