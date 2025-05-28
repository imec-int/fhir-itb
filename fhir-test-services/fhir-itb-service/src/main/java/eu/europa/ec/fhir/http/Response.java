package eu.europa.ec.fhir.http;

import java.net.http.HttpHeaders;
import java.util.Optional;

/**
 * An HTTP response data.
 *
 * @param status  The HTTP status code.
 * @param body    The response's body.
 * @param headers The returned headers.
 */
public record Response(int status, String body, HttpHeaders headers) {

    public Optional<String> contentType() {
        return headers.allValues(org.springframework.http.HttpHeaders.CONTENT_TYPE)
                .stream()
                .findFirst();
    }

}
