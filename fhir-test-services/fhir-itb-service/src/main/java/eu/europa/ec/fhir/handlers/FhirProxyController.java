package eu.europa.ec.fhir.handlers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europa.ec.fhir.gitb.DeferredRequestMapper;
import eu.europa.ec.fhir.gitb.api.model.StartSessionRequestPayload;
import eu.europa.ec.fhir.http.RequestParams;
import eu.europa.ec.fhir.proxy.DeferredRequest;
import eu.europa.ec.fhir.proxy.FhirProxyServiceHelper;
import eu.europa.ec.fhir.proxy.FhirRefCodes;
import eu.europa.ec.fhir.proxy.ItbRestClient;
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
    private final ObjectMapper objectMapper;

    private static final Logger LOGGER = LoggerFactory.getLogger(FhirProxyController.class);

    private final ItbRestClient itbRestClient;
    private final FhirProxyServiceHelper fhirProxyServiceHelper;
    private final DeferredRequestMapper deferredRequests;

    private final FhirRefCodes fhirRefCodes;

    public FhirProxyController(FhirRefCodes fhirRefCodes, ItbRestClient itbRestClient, FhirProxyServiceHelper fhirProxyServiceHelper, DeferredRequestMapper deferredRequests, RestClient restClient, ObjectMapper objectMapper) {
        this.fhirRefCodes = fhirRefCodes;
        this.itbRestClient = itbRestClient;
        this.fhirProxyServiceHelper = fhirProxyServiceHelper;
        this.deferredRequests = deferredRequests;
        this.objectMapper = objectMapper;
    }

    /**
     * Extracts the reference code from the JSON payload based on the resource
     * type.
     */
    private Optional<String> getReferenceCode(String json, String resourceType) {
        var referenceCodePath = this.fhirRefCodes.get(resourceType);
        if (referenceCodePath.isEmpty()) {
            LOGGER.warn("No reference code path found for resource type: {}", resourceType);
            return Optional.empty();
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            LOGGER.warn("Failed to parse JSON: {}", e.getMessage());
            return Optional.empty();
        }

        JsonNode jsonReferenceCode = root.at(referenceCodePath.get());
        return jsonReferenceCode.isMissingNode() ?
                Optional.empty() :
                Optional.ofNullable(jsonReferenceCode.asText());
    }

    @RequestMapping({"/proxy/{resourceType}", "/proxy/{resourceType}/{id}"})
    public DeferredResult<ResponseEntity<String>> handleRequest(
            HttpServletRequest request,
            @PathVariable("resourceType") String resourceType,
            @PathVariable(value = "id", required = false) Optional<String> resourceId,
            @RequestBody(required = false) Optional<String> payload
    ) {
        String fullPath = String.format("%s%s", resourceType, resourceId.map(value -> "/" + value)
                .orElse(""));
        RequestParams proxyRequestParams = fhirProxyServiceHelper.toFhirHttpParams(request, fullPath, payload);
        Optional<String> referenceCode = payload.flatMap(body -> getReferenceCode(body, resourceType));

        String testId = String.format("%s-%s%s",
                proxyRequestParams.method().toString().toLowerCase(),
                resourceType.replace("/", ""),
                referenceCode.map(code -> "-" + code).orElse("")
        );

        LOGGER.debug("Starting test session(s) for \"{}\"", testId);

        var deferredResult = new DeferredResult<ResponseEntity<String>>();
        var deferredRequest = new DeferredRequest(proxyRequestParams, deferredResult);

        try {
            // start test sessions and defer the request
            var startSessionPayload = StartSessionRequestPayload.fromRequestParams(new String[]{testId}, proxyRequestParams);
            var itbResponse = itbRestClient.startSession(startSessionPayload);
            var createdSessions = itbResponse.createdSessions();
            var sessionId = createdSessions[0].session();

            LOGGER.info("Test session(s) created: {}", (Object[]) createdSessions);
            deferredRequests.put(sessionId, deferredRequest);
        } catch (Exception e) {
            LOGGER.warn("Failed to start test session(s): {}", e.getMessage());
            deferredRequest.resolve();

            try {
                testId = String.format("%s-%s", proxyRequestParams.method().toString().toLowerCase(), resourceType.replace("/", ""));
                LOGGER.info("Intiating general test session(s), testId:" + testId);
                deferredResult = new DeferredResult<ResponseEntity<String>>();
                deferredRequest = new DeferredRequest(proxyRequestParams, deferredResult);
                var startSessionPayload = StartSessionRequestPayload.fromRequestParams(new String[]{testId}, proxyRequestParams);
                var itbResponse = itbRestClient.startSession(startSessionPayload);
                var createdSessions = itbResponse.createdSessions();
                var sessionId = createdSessions[0].session();
                LOGGER.info("Test session(s) created: {}", (Object[]) createdSessions);
                deferredRequests.put(sessionId, deferredRequest);
            } catch (Exception ec) {
                LOGGER.warn("Failed to start test session(s): {}", ec.getMessage());
                deferredRequest.resolve();
            }


        }

        return deferredResult;
    }
}
