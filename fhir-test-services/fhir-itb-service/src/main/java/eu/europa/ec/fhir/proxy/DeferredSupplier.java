package eu.europa.ec.fhir.proxy;

import org.springframework.web.context.request.async.DeferredResult;

import java.util.function.Supplier;

/**
 * Represents a {@link Supplier} that can be called to set the result of a {@link DeferredResult}.
 *
 * @param <T>
 */
public class DeferredSupplier<T> implements Supplier<T> {

    private final DeferredResult<T> deferredResult;
    private final Supplier<T> supplier;

    public DeferredSupplier(DeferredResult<T> deferredResult, Supplier<T> resultSupplier) {
        this.deferredResult = deferredResult;
        this.supplier = resultSupplier;
    }

    /**
     * Calls the {@link Supplier} and sets the result of the {@link DeferredResult}.
     * If the result has already been set, the result is returned without calling the supplier.
     * Throws an {@link IllegalStateException} if the result has expired.
     */
    public T get() throws IllegalStateException {
        if (deferredResult.isSetOrExpired()) {
            if (deferredResult.hasResult()) {
                return (T) deferredResult.getResult();
            }
            // the result has expired so we should avoid calling the supplier
            // to avoid unexpected side effects
            throw new IllegalStateException("Deferred result has expired");
        }

        T result = supplier.get();
        deferredResult.setResult(result);
        return result;
    }
}
