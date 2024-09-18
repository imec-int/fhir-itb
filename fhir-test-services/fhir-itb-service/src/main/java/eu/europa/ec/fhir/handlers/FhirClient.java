package eu.europa.ec.fhir.handlers;

import eu.europa.ec.fhir.gitb.MessagingServiceImpl;
import eu.europa.ec.fhir.handlers.RequestResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * Component used to make calls to FHIR servers.
 */
@Component
public class FhirClient {

    @Value("${fhir.contentTypeFull}")
    private String fhirContentType;

    private static final String PATIENT_IDENTIFIER_PREFIX = "https://www.ehealth.fgov.be/standards/fhir/core/NamingSystem/ssin|urn:be:fgov:ehealth:pseudo:v1:";
    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(FhirClient.class);
    /**
     * Call the FHIR server at the specific URI and return the response.
     *
     * @param method The HTTP method to use.
     * @param uri The full URI to use.
     * @param payload The body payload as a string (optional).
     * @param authorizationToken The authorization token to use for the request (optional).
     * @param patientIdentifier The patient identifier (to be appended to the fixed prefix, optional).
     * @return The result of the call.
     */
    public RequestResult callServer(HttpMethod method, String uri, String payload, String authorizationToken, String patientIdentifier) {
        // Construct payload based on the presence of patientIdentifier
        if (patientIdentifier != null && !patientIdentifier.isEmpty()) {
            String fullPatientIdentifier = PATIENT_IDENTIFIER_PREFIX + patientIdentifier;
            payload = (payload != null && !payload.isEmpty())
                    ? "patient.identifier=" + fullPatientIdentifier + "&" + payload
                    : "patient.identifier=" + fullPatientIdentifier;
        } else if (payload == null || payload.isEmpty()) {
            payload = "";
        }



        var builder = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .method(method.name(), HttpRequest.BodyPublishers.ofString(payload));

        // Add necessary headers
        builder = builder.header(HttpHeaders.ACCEPT, fhirContentType)
                .header(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");

        // Add authorization token if present
        if (authorizationToken != null && !authorizationToken.isEmpty()) {
            builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + authorizationToken);
        }

        // Always include no-cache header
        builder.header(HttpHeaders.CACHE_CONTROL, "no-cache");

        var request = builder.build();
        LOG.info("HTTP request" + request.toString());

        try {
            var response = HttpClient.newBuilder()
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            LOG.info(response.body()+response.headers());
            return new RequestResult(response.statusCode(), response.body(), response.headers());
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException(String.format("Error while calling endpoint [%s]", uri), e);
        }
    }
}

