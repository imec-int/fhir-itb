package eu.europa.ec.fhir.proxy;

import eu.europa.ec.fhir.http.RequestParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.function.Supplier;

/**
 * Represents a deferred HTTP Request that is linked to a {@link DeferredResult}.
 * Call 'resolve' to perform the request and set the result of the {@link DeferredResult}.
 */
public class DeferredRequest {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeferredRequest.class);

    private final RestClient restClient = RestClient.builder().build();

    private final RequestParams requestParams;
    private final DeferredResult<ResponseEntity<String>> deferredResult;

    public DeferredRequest(RequestParams requestParams, DeferredResult<ResponseEntity<String>> deferredResult) {
        this.requestParams = requestParams;
        this.deferredResult = deferredResult;
    }

    public RequestParams getRequestParams() {
        return requestParams;
    }

    /**
     * Calls the {@link Supplier} and sets the result of the {@link DeferredResult}.
     * If the result has already been set, the result is returned without calling the supplier.
     * Throws an {@link IllegalStateException} if the result has expired.
     */
    public ResponseEntity<String> resolve() throws IllegalStateException {
        if (deferredResult.isSetOrExpired()) {
            if (deferredResult.hasResult()) {
                return (ResponseEntity<String>) deferredResult.getResult();
            }
            // the result has expired so we should avoid calling the supplier
            // to avoid unexpected side effects
            throw new IllegalStateException("Deferred result has expired.");
        }

        // proxy the request
        var spec = restClient
                .method(requestParams.method())
                .uri(requestParams.uri())
                .headers(headers -> headers.addAll(requestParams.headers()));

        if (requestParams.body() != null) {
            spec.body(requestParams.body());
        }

        ResponseEntity<String> result;
        try {
            result = spec.retrieve()
                    // don't care about the result, just forward the response
                    .onStatus(status -> true, (req, res) -> {})
                    .toEntity(String.class);
        } catch (Exception e) {
            LOGGER.warn("Failed to proxy request: [{}]. Error: [{}]", requestParams, e.getMessage());
            result = ResponseEntity.status(500).body("Failed to proxy request");
        }

        deferredResult.setResult(result);
        return result;
    }

}
