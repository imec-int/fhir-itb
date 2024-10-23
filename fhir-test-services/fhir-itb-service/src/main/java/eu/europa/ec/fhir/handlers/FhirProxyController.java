package eu.europa.ec.fhir.handlers;

import eu.europa.ec.fhir.gitb.DeferredRequestMapper;
import eu.europa.ec.fhir.gitb.api.model.StartSessionRequestPayload;
import eu.europa.ec.fhir.http.RequestParams;
import eu.europa.ec.fhir.proxy.DeferredRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.Optional;

/**
 * Triggers test runs based on request parameters.
 */
@RestController
public class FhirProxyController {

    private static final Logger LOGGER = LoggerFactory.getLogger(FhirProxyController.class);

    private final ItbRestClient itbRestClient;
    private final FhirProxyService fhirProxyService;
    private final DeferredRequestMapper deferredRequests;

    public FhirProxyController(ItbRestClient itbRestClient, FhirProxyService fhirProxyService, DeferredRequestMapper deferredRequests, RestClient restClient) {
        this.itbRestClient = itbRestClient;
        this.fhirProxyService = fhirProxyService;
        this.deferredRequests = deferredRequests;
    }

    @RequestMapping({"/proxy/{resourceType}", "/proxy/{resourceType}/{id}"})
    public DeferredResult<ResponseEntity<String>> handleRequest(
            HttpServletRequest request,
            @PathVariable("resourceType") String resourceType,
            @PathVariable(value = "id", required = false) Optional<String> resourceId,
            @RequestBody(required = false) String body
    ) {
        var fullPath = String.format("%s%s", resourceType, resourceId.map(value -> "/" + value).orElse(""));
        RequestParams proxyRequestParams = fhirProxyService.getFhirHttpParams(request, fullPath, body);

        String testId = String.format("%s-%s", proxyRequestParams.method().toString().toLowerCase(), resourceType.replace("/", ""));

        LOGGER.debug("Starting test session(s) for \"{}\"", testId);

        var deferredResult = new DeferredResult<ResponseEntity<String>>();
        var deferredRequest = new DeferredRequest(proxyRequestParams, deferredResult);

        try {
            // start test sessions and defer the request
            var startSessionPayload = StartSessionRequestPayload.fromRequestParams(new String[]{testId}, proxyRequestParams);
            var itbResponse = itbRestClient.startSession(startSessionPayload);
            var createdSessions = itbResponse.createdSessions();
            var sessionId = createdSessions[0].session();

            LOGGER.info("Test session(s) {} created!", (Object[]) createdSessions);
            deferredRequests.put(sessionId, deferredRequest);
        } catch (Exception e) {
            LOGGER.warn("Failed to start test session(s): {}", e.getMessage());
            deferredRequest.resolve();
        }

        return deferredResult;
    }
}
