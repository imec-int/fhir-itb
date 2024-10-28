package eu.europa.ec.fhir.http;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import java.net.URI;

/**
 * The required parameters for an HTTP request.
 */
public record RequestParams(
        URI uri,
        HttpMethod method,
        HttpHeaders headers,
        String body
) {}
