package eu.europa.ec.fhir.proxy;

import eu.europa.ec.fhir.http.RequestParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.async.DeferredResult;

/**
 * Represents a deferred HTTP Request that is linked to a {@link DeferredResult}.
 * Call {@link #resolve()} to perform the request and set the result of the {@link DeferredResult}.
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
     * If the result has already been set, the existing result is returned.
     * If not, then it performs the request and sets the result of the {@link DeferredResult}.
     * Throws an {@link IllegalStateException} if the result is expired.
     */
    public ResponseEntity<String> resolve() throws IllegalStateException {
        if (deferredResult.isSetOrExpired()) {
            if (deferredResult.hasResult()) {
                // SAFETY: we check for a result above, and the result type is
                //  well-defined as ResponseEntity<String>
                return (ResponseEntity<String>) deferredResult.getResult();
            }
            // the result has expired so we don't perform the request
            // to avoid side effects
            throw new IllegalStateException("Deferred result has expired.");
        }

        return sendRequest();
    }

    /**
     * Performs the request and sets the deferred result.
     */
    private ResponseEntity<String> sendRequest() {
        var spec = restClient
                .method(requestParams.method())
                .uri(requestParams.uri())
                .headers(headers -> headers.addAll(requestParams.headers()));

        requestParams.body().ifPresent(spec::body);

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
