package eu.europa.ec.fhir.handlers;

import eu.europa.ec.fhir.utils.HttpUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class FhirProxyService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FhirProxyService.class);

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

    public ResponseEntity<String> proxyRequest(HttpServletRequest request, String path) throws IOException, InterruptedException {
        var fhirRequest = buildFhirRequest(request, path);

        HttpClient client = HttpClient.newHttpClient();
        var response = client.send(fhirRequest, HttpResponse.BodyHandlers.ofString());

        return ResponseEntity.status(response.statusCode())
                // TODO: include response headers
                .body(response.body());
    }
}
