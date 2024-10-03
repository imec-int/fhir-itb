package eu.europa.ec.fhir.handlers;

import eu.europa.ec.fhir.utils.HttpUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
