package eu.europa.ec.fhir.handlers;

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

    /**
     * Call the FHIR server at the specific URI and return the response.
     *
     * @param method The HTTP method to use.
     * @param uri The full URI to use.
     * @param body The body payload (optional).
     * @return The result of the call.
     */
    public RequestResult callServer(HttpMethod method, String uri, String body) {
        var builder = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .method(method.name(), (body == null)?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body));
        if (method != HttpMethod.DELETE) {
            builder = builder.header(HttpHeaders.ACCEPT, fhirContentType);
            if (method == HttpMethod.PUT || method == HttpMethod.POST) {
                builder = builder.header(HttpHeaders.CONTENT_TYPE, fhirContentType);
            }
        }
        var request = builder.build();
        try {
            var response = HttpClient.newBuilder()
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new RequestResult(response.statusCode(), response.body(), response.headers());
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException(String.format("Error while calling endpoint [%s]", uri), e);
        }
    }

}
