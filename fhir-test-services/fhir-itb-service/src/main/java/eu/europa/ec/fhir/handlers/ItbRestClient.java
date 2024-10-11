package eu.europa.ec.fhir.handlers;

import com.gitb.core.AnyContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

import java.io.IOException;

public class ItbRestClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(ItbRestClient.class);


    public record StartSessionResponse(
            SessionInfo[] createdSessions
    ) {
    }

    public record StartSessionRequestPayload(
            String[] testCase,
            InputMapping[] inputMapping) {
    }

    public record SessionInfo(
            String testSuite,
            String testCase,
            String session
    ) {
    }

    public record InputMapping(AnyContent input) {
    }

    private final RestClient restClient;

    public ItbRestClient(RestClient restClient, String ITB_API_KEY) {
        // TODO: get from configuration
        this.restClient = restClient;
    }

    /**
     * Starts a test session in ITB.
     */
    public StartSessionResponse startSession(StartSessionRequestPayload payload) throws IOException {

        var req = restClient
                .post()
                .uri("/tests/start")
                .body(payload);

        return req.exchange((request, response) -> {
            if (response.getStatusCode().is2xxSuccessful()) {
                var responsePayload = response.bodyTo(StartSessionResponse.class);
                if (responsePayload == null) {
                    throw new IOException("Invalid response from ITB API.");
                }

                return responsePayload;
            } else {
                throw new IOException("Failed to start test session(s): " + response.bodyTo(String.class));
            }
        });
    }
}
