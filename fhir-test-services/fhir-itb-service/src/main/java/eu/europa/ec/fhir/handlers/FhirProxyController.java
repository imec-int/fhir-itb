package eu.europa.ec.fhir.handlers;

import com.gitb.core.AnyContent;
import com.gitb.core.ValueEmbeddingEnumeration;
import eu.europa.ec.fhir.handlers.ItbRestClient.InputMapping;
import eu.europa.ec.fhir.handlers.ItbRestClient.StartSessionRequestPayload;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Triggers test runs based on request parameters.
 */
@RestController
public class FhirProxyController {

    private static final Logger LOGGER = LoggerFactory.getLogger(FhirProxyController.class);

    private final ItbRestClient itbRestClient;

    public FhirProxyController(ItbRestClient itbRestClient) {
        this.itbRestClient = itbRestClient;
    }

    @RequestMapping(value = "/proxy/{*path}")
    public ResponseEntity<Void> handleRequest(
            @RequestBody(required = false) String body,
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable("path") String path,
            HttpServletRequest request
    ) {
        var requestMethod = request.getMethod();
        var testId = String.format("%s%s", requestMethod.toLowerCase(), path.replace("/", "-"));
        LOGGER.debug("Starting test session(s) for \"{}\"", testId);

        var requestBodyInput = new AnyContent();
        requestBodyInput.setName("requestBody");
        requestBodyInput.setValue(body);
        requestBodyInput.setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);

        var requestTokenInput = new AnyContent();
        requestTokenInput.setName("requestToken");
        requestTokenInput.setValue(token);
        requestTokenInput.setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);

        var requestMethodInput = new AnyContent();
        requestMethodInput.setName("requestMethod");
        requestMethodInput.setValue(requestMethod);
        requestMethodInput.setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);

        var startSessionPayload = new StartSessionRequestPayload(
                new String[]{testId},
                new ItbRestClient.InputMapping[]{
                        new InputMapping(requestBodyInput),
                        new InputMapping(requestTokenInput),
                        new InputMapping(requestMethodInput)
                }
        );

        // TODO: extract session ID from itbResponse
        try {
            var itbResponse = itbRestClient.startSession(startSessionPayload);
            LOGGER.info("Test session(s) {} created!", (Object[]) itbResponse.createdSessions());
        } catch (Exception e) {
            LOGGER.warn("Failed to start test session(s): {}", e.getMessage());
            // TODO: proxy request through
        }


        // TODO: Use a DeferredResult to wait for the TestCase to trigger the request.
        //  Store the DeferredResult in a HashMap, linked to the test session and request input.
        //  We can use (some of) {sessionId, payload, method, path, and token} as key.
        //  Once the request is actually performed, retrieve the DeferredResult from the HashMap,
        //  and set its value to the response from the request.

        return ResponseEntity.noContent().build();
    }
}
