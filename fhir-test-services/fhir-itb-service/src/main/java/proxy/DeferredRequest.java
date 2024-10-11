package proxy;

import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.async.DeferredResult;

/**
 * Defers the exchange of a {@link RestClient.RequestBodySpec}.
 * TODO: support generic types (ResponseEntity<T>)
 */
public class DeferredRequest extends DeferredResult<ResponseEntity<String>> {

    private final RestClient.RequestBodySpec request;

    public DeferredRequest(RestClient.RequestBodySpec request) {
        this.request = request;
    }

    /**
     * Performs the exchange and set the result.
     */
    public void exchange() throws IllegalStateException {
        if (isSetOrExpired()) {
            throw new IllegalStateException("Result already set or expired!");
        }

        // TODO: handle exceptions from exchange
        var response = request.retrieve().toEntity(String.class);
        setResult(response);
    }
}
