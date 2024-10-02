package eu.europa.ec.fhir.handlers;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

final class HttpUtils {
    // TODO: find out which headers are in fact restricted
    private static final String[] RESTRICTED_HEADERS = {"host", "connection", "content-length", "accept-encoding", "accept-charset", "transfer-encoding", "accept-encoding"};
    private static final String[] RESTRICTED_HEADERS_PREFIXES = {"proxy-", "sec-"};

    public static boolean isRestrictedHeader(String headerName) {
        return Arrays.stream(RESTRICTED_HEADERS)
                .anyMatch(headerName::equalsIgnoreCase)
                || Arrays.stream(RESTRICTED_HEADERS_PREFIXES)
                .anyMatch(headerName::startsWith);
    }

    public static HttpHeaders extractHeaders(HttpServletRequest request) {
        return Collections.list(request.getHeaderNames()).stream()
                .collect(HttpHeaders::new, (httpHeaders, headerName) -> {
                    Collections.list(request.getHeaders(headerName))
                            .forEach(value -> {
                                if (!isRestrictedHeader(headerName)) {
                                    httpHeaders.add(headerName, value);
                                }
                            });
                }, HttpHeaders::putAll);
    }

    public static HttpRequest.BodyPublisher getBodyPublisher(HttpServletRequest request) {
        try {
            var inputStream = request.getInputStream();
            return HttpRequest.BodyPublishers.ofInputStream(() -> inputStream);
        } catch (IOException e) {
            return HttpRequest.BodyPublishers.noBody();
        }
    }

    /**
     * Return the query string of the request, including the leading '?'.
     */
    public static Optional<String> getQueryString(HttpServletRequest request) {
        return Optional.ofNullable(request.getQueryString())
                .map(queryString -> "?" + queryString);

    }
}

/**
 * Proxies all requests as they come to the configured FHIR server and
 * runs automated tests against their responses.
 */
@RestController
public class FhirProxy {

    private static final Logger LOGGER = LoggerFactory.getLogger(FhirProxy.class);

    @Value("${fhir.proxy.endpoint}")
    private String fhirProxyEndpoint;

    private URI buildFhirServerURI(HttpServletRequest request, String path) {
        String queryString = HttpUtils.getQueryString(request).orElse("");
        String uriString = String.format("%s/%s%s", fhirProxyEndpoint, path, queryString);

        try {
            return new URL(uriString).toURI();
        } catch (Exception e) {
            LOGGER.error("Invalid Proxy URI \"{}\": {}", uriString, e.getMessage());
            throw new IllegalStateException("Invalid Proxy URI");
        }
    }

    private HttpRequest buildFhirRequest(HttpServletRequest request, String path) {
        var requestBuilder = HttpRequest.newBuilder()
                .uri(buildFhirServerURI(request, path))
                .method(request.getMethod(), HttpUtils.getBodyPublisher(request));

        HttpUtils.extractHeaders(request)
                .forEach((header, values) -> values.forEach(value -> requestBuilder.header(header, value)));

        return requestBuilder.build();
    }

    @RequestMapping(value = "/proxy/{path}")
    public ResponseEntity<String> handleRequest(
            HttpServletRequest request,
            @PathVariable String path
    ) throws IOException, InterruptedException {
        var fhirRequest = buildFhirRequest(request, path);

        HttpClient client = HttpClient.newHttpClient();
        var response = client.send(fhirRequest, HttpResponse.BodyHandlers.ofString());

        // TODO: trigger automated tests against the response

        // Return the response as-is
        return ResponseEntity.status(response.statusCode())
                // TODO: include response headers
                .body(response.body());
    }
}
