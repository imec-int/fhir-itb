package eu.europa.ec.fhir.handlers;

import com.gitb.core.AnyContent;
import com.gitb.core.ValueEmbeddingEnumeration;
import eu.europa.ec.fhir.gitb.DeferredRequestMapper;
import eu.europa.ec.fhir.handlers.ItbRestClient.InputMapping;
import eu.europa.ec.fhir.handlers.ItbRestClient.StartSessionRequestPayload;
import eu.europa.ec.fhir.proxy.DeferredRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

/**
 * Triggers test runs based on request parameters.
 */
@RestController
public class FhirProxyController {

    private static final Logger LOGGER = LoggerFactory.getLogger(FhirProxyController.class);

    private final ItbRestClient itbRestClient;
    private final FhirProxyService fhirProxyService;
    private final DeferredRequestMapper deferredRequestMapper;

    public FhirProxyController(ItbRestClient itbRestClient, FhirProxyService fhirProxyService, DeferredRequestMapper deferredRequestMapper) {
        this.itbRestClient = itbRestClient;
        this.fhirProxyService = fhirProxyService;
        this.deferredRequestMapper = deferredRequestMapper;
    }

    @RequestMapping(value = "/proxy/{*path}")
    public DeferredResult<ResponseEntity<String>> handleRequest(
            HttpServletRequest request,
            @PathVariable("path") String path,
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody(required = false) String body
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

        // TODO: set some reasonable timeout
        var deferred = new DeferredRequest(fhirProxyService.buildRequest(request, path, body));
        try {
            var itbResponse = itbRestClient.startSession(startSessionPayload);
            var sessionId = itbResponse.createdSessions()[0].session();
            LOGGER.info("Test session(s) {} created!", (Object[]) itbResponse.createdSessions());
            deferredRequestMapper.put(sessionId, deferred);
        } catch (Exception e) {
            LOGGER.warn("Failed to start test session(s): {}", e.getMessage());
            // if the tests cannot run, perform the exchange directly
            deferred.exchange();
        }

        return deferred;
    }
}
