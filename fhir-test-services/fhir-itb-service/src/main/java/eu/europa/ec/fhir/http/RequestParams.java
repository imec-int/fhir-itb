package eu.europa.ec.fhir.http;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import java.net.URI;
import java.util.Optional;

/**
 * The required parameters for an HTTP request.
 */
public record RequestParams(
        URI uri,
        HttpMethod method,
        HttpHeaders headers,
        Optional<String> body
) {}
