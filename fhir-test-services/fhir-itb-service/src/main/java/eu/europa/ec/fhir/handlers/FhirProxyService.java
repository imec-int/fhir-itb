package eu.europa.ec.fhir.handlers;

import eu.europa.ec.fhir.gitb.DeferredRequestMapper;
import eu.europa.ec.fhir.utils.HttpUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
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

    public FhirProxyService(RestClient restClient, DeferredRequestMapper deferredRequestMapper) {
        this.restClient = restClient;
    }

    private URI buildURI(HttpServletRequest request, String baseUrl, String path) {
        String queryString = HttpUtils.getQueryString(request).orElse("");
        String uriString = String.format("%s/%s%s", baseUrl, path, queryString);

        try {
            return new URL(uriString).toURI();
        } catch (Exception e) {
            LOGGER.error("Invalid Proxy URI: {}", uriString);
            throw new IllegalArgumentException("Invalid URI: " + uriString);
        }
    }

    public RestClient.RequestBodySpec buildRequest(HttpServletRequest request, String path, String payload) {
        var spec = restClient
                .method(HttpMethod.valueOf(request.getMethod()))
                .uri(buildURI(request, fhirProxyEndpoint, path))
                .headers((headers) -> {
                    HttpUtils.copyHeaders(request, headers);
                });

        if (payload != null) {
            spec.body(payload);
        }

        return spec;
    }
}
