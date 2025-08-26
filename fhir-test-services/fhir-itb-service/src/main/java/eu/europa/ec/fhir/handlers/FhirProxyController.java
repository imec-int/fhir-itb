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

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

    private String getResourceTypeFromJson(String json, String resourceType) {
        var referenceCodePath = this.fhirRefCodes.get(resourceType);
        if (referenceCodePath.isEmpty()) {
            LOGGER.warn("No reference code path found for resource type: {}", resourceType);
            return "";
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            LOGGER.warn("Failed to parse JSON: {}", e.getMessage());
            return "";
        }

        JsonNode jsonReferenceCode = root.at(referenceCodePath.get());
        if (jsonReferenceCode.isMissingNode() || jsonReferenceCode.isNull()) {
            return ""; // Return empty string if node doesn't exist or is explicitly null
        }

        return jsonReferenceCode.asText();
    }

    //private String get

    @RequestMapping({
            "/proxy",                                    // system-level (e.g., Bundle POST)
            "/proxy/{resourceType}",                     // e.g., /proxy/Patient?name=...
            "/proxy/{resourceType}/",                    // trailing slash
            "/proxy/{resourceType}/{id}",                // e.g., /proxy/Patient/123
            "/proxy/{resourceType}/_search",             // POST-based search
            "/proxy/{resourceType}/_search/"             // trailing slash
    })
    public DeferredResult<ResponseEntity<String>> handleRequest(
            HttpServletRequest request,
            @PathVariable(value = "resourceType", required = false) Optional<String> resourceTypeOpt,
            @PathVariable(value = "id",           required = false) Optional<String> resourceId,
            @RequestBody(required = false) Optional<String> payload
    ) {
        RequestParams proxyRequestParams;
        List<Map.Entry<String, RequestParams>> testIds = new ArrayList<>();

        // Detect if this is a Bundle (system-level POST)
        String resourceTypeFromBody = payload.map(b -> getResourceTypeFromJson(b, "resourceType")).orElse("");
        boolean isBundle = "Bundle".equals(resourceTypeFromBody);

        // Resolve resourceType: prefer path variable, fall back to body only for Bundle
        String resourceType = resourceTypeOpt.orElse(isBundle ? "Bundle" : "");

        // Special handling for POST /{type}/_search (body is usually form-encoded, not JSON)
        boolean isPostSearch = request.getRequestURI().matches(".*/_search/?$");

        if (isBundle) {
            // Your existing bundle logic (unchanged)
            try {
                JsonNode root = objectMapper.readTree(payload.orElse("{}"));
                JsonNode entries = root.at(this.fhirRefCodes.get("entry").get());
                if (entries.isArray()) {
                    for (JsonNode entry : entries) {
                        JsonNode resourceNode = entry.get("resource");
                        String entryResourceType = resourceNode != null && resourceNode.has("resourceType")
                                ? resourceNode.get("resourceType").asText()
                                : "";

                        String method = Optional.ofNullable(entry.get("request"))
                                .map(r -> r.get("method"))
                                .map(JsonNode::asText)
                                .map(String::toLowerCase)
                                .orElse("");

                        String vaccineCode = "";
                        if (resourceNode != null && resourceNode.has("vaccineCode")) {
                            JsonNode coding = resourceNode.path("vaccineCode").path("coding");
                            if (coding.isArray() && coding.size() > 0) {
                                vaccineCode = coding.get(0).path("code").asText("");
                            }
                        }

                        testIds.add(new AbstractMap.SimpleEntry<>(
                                method + "-" + entryResourceType + "-" + vaccineCode,
                                fhirProxyServiceHelper.toFhirHttpParams(request, "", Optional.ofNullable(resourceNode).map(JsonNode::toString))
                        ));
                    }
                }
            } catch (JsonProcessingException e) {
                LOGGER.warn("Failed to parse Bundle entries: {}", e.getMessage());
            }
            proxyRequestParams = fhirProxyServiceHelper.toFhirHttpParams(request, "", payload);

        } else {
            if (resourceType.isBlank()) {
                // We could not determine the resource type; this causes the “actor” error downstream.
                // Bail out early with a clear log (or consider mapping to a default/“general” actor).
                LOGGER.warn("Resource type is missing. Ensure @RequestMapping uses {resourceType} instead of *.");
            }

            // Build the path passed to the proxy target (/Patient, /Patient/123, /Patient/_search, etc.)
            String fullPath;
            if (isPostSearch) {
                fullPath = resourceType + "/_search";
            } else if (resourceId.isPresent()) {
                fullPath = resourceType + "/" + resourceId.get();
            } else {
                fullPath = resourceType; // e.g., GET /Patient?name=...
            }

            proxyRequestParams = fhirProxyServiceHelper.toFhirHttpParams(request, fullPath, payload);

            Optional<String> referenceCode = payload.flatMap(body -> getReferenceCode(body, resourceType));
            String testKey = String.format("%s-%s%s",
                    proxyRequestParams.method().toString().toLowerCase(),
                    resourceType.replace("/", ""),
                    referenceCode.map(code -> "-" + code).orElse("")
            );
            testIds.add(new AbstractMap.SimpleEntry<>(testKey, proxyRequestParams));
        }

        LOGGER.info("Starting test session(s) for \"{}\"", testIds);
        var deferredResult = new DeferredResult<ResponseEntity<String>>();
        var deferredRequest = new DeferredRequest(proxyRequestParams, deferredResult);

        for (Map.Entry<String, RequestParams> testId : testIds) {
            try {
                var startSessionPayload = StartSessionRequestPayload.fromRequestParams(
                        new String[]{testId.getKey()}, testId.getValue());
                var itbResponse = itbRestClient.startSession(startSessionPayload);
                var sessionId = itbResponse.createdSessions()[0].session();
                deferredRequests.put(sessionId, deferredRequest);
            } catch (Exception e) {
                LOGGER.warn("Failed to start test session(s) for testId {}: {}", testId, e.getMessage());
                deferredRequest.resolve();
                try {
                    var startSessionPayload = StartSessionRequestPayload.fromRequestParams(
                            new String[]{testId.getKey().replaceAll("-[^-]*$", "")}, testId.getValue());
                    var itbResponse = itbRestClient.startSession(startSessionPayload);
                    var sessionId = itbResponse.createdSessions()[0].session();
                    deferredRequests.put(sessionId, deferredRequest);
                } catch (Exception ec) {
                    LOGGER.warn("Failed to start general test session(s): {}", ec.getMessage());
                    deferredRequest.resolve();
                }
            }
        }

        return deferredResult;
    }
}