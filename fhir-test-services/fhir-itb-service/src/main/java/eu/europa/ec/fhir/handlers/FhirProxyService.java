package eu.europa.ec.fhir.handlers;

import eu.europa.ec.fhir.http.HttpParams;
import eu.europa.ec.fhir.http.HttpUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URL;

@Service
public class FhirProxyService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FhirProxyService.class);

    @Value("${fhir.proxy.endpoint}")
    private String fhirProxyEndpoint;

    private URI buildFhirURI(HttpServletRequest request, String baseUrl, String path) {
        String queryString = HttpUtils.getQueryString(request).orElse("");
        String uriString = String.format("%s%s%s", baseUrl, path, queryString);

        try {
            return new URL(uriString).toURI();
        } catch (Exception e) {
            LOGGER.error("Invalid Proxy URI: {}", uriString);
            throw new IllegalArgumentException("Invalid URI: " + uriString);
        }
    }

    public HttpParams getFhirHttpParams(HttpServletRequest request, String path, String body) {
        return new HttpParams(
                buildFhirURI(request, fhirProxyEndpoint, path),
                HttpMethod.valueOf(request.getMethod()),
                HttpUtils.cloneHeaders(request),
                body
        );
    }
}
