package be.smals.vas.integrations.helper.utils;

/**
 * Represents a supplier of results having one argument.
 *
 * <p>There is no requirement that a new or distinct result be returned each
 * time the supplier is invoked.
 *
 * <p>This is a <a href="package-summary.html">functional interface</a>
 * whose functional method is {@link #get(T)}.
 *
 * @param <T> the type of arguments supplied by this supplier
 * @param <R> the type of results supplied by this supplier
 * @since 1.8
 */
@FunctionalInterface
public interface SupplierWithParameter<T, R> {

  /**
   * Gets a result.
   *
   * @param t an argument
   * @return a result
   */
  R get(T t);
}
