package eu.europa.ec.fhir.handlers;

import eu.europa.ec.fhir.utils.HttpUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.URL;

@Service
public class FhirProxyService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FhirProxyService.class);

    private final RestClient restClient;

    @Value("${fhir.proxy.endpoint}")
    private String fhirProxyEndpoint;

    public FhirProxyService(RestClient restClient) {
        this.restClient = restClient;
    }

    public URI buildURI(HttpServletRequest request, String baseUrl, String path) {
        String queryString = HttpUtils.getQueryString(request).orElse("");
        String uriString = String.format("%s/%s%s", baseUrl, path, queryString);

        try {
            return new URL(uriString).toURI();
        } catch (Exception e) {
            LOGGER.error("Invalid Proxy URI: {}", uriString);
            throw new IllegalArgumentException("Invalid URI: " + uriString);
        }
    }

    /**
     * Proxies a request to an endpoint in the configured FHIR server, copying the method, headers, and query parameters from the given request.
     */
    public ResponseEntity<String> proxyRequest(HttpServletRequest request, String path, String body) {
        var spec = restClient
                .method(HttpMethod.valueOf(request.getMethod()))
                .uri(buildURI(request, fhirProxyEndpoint, path))
                .headers((headers) -> {
                    HttpUtils.extractHeaders(request)
                            .forEach((header, values) -> values.forEach(value -> headers.add(header, value)));
                });


        if (body != null) {
            spec.body(body);
        }

        return spec.retrieve().toEntity(String.class);
    }
}
