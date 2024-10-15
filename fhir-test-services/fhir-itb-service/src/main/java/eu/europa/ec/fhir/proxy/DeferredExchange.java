package eu.europa.ec.fhir.proxy;

import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.async.DeferredResult;

/**
 * Defers the exchange of a {@link RestClient.RequestBodySpec}.
 * TODO: support generic types (ResponseEntity<T>)
 */
public class DeferredExchange extends DeferredResult<ResponseEntity<String>> {

    private final RestClient.RequestBodySpec request;

    public DeferredExchange(RestClient.RequestBodySpec request) {
        this.request = request;
    }

    /**
     * Performs the exchange and set the DeferredResult.
     */
    public ResponseEntity<String> exchange() throws IllegalStateException {
        if (isSetOrExpired()) {
            throw new IllegalStateException("Result already set or expired!");
        }

        var response = request
                .retrieve()
                // don't care about the result, just forward the response
                .onStatus(status -> true, (req, res) -> {})
                .toEntity(String.class);

        setResult(response);
        return response;
    }
}
