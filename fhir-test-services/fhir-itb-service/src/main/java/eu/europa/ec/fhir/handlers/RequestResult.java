package eu.europa.ec.fhir.handlers;

import java.net.http.HttpHeaders;
import java.util.Optional;

/**
 * Record reflecting a request's result.
 *
 * @param status The HTTP status code.
 * @param body The response's body.
 * @param headers The returned headers.
 */
public record RequestResult(int status, String body, HttpHeaders headers) {

    public Optional<String> contentType() {
        return headers.allValues(org.springframework.http.HttpHeaders.CONTENT_TYPE).stream().findFirst();
    }

}