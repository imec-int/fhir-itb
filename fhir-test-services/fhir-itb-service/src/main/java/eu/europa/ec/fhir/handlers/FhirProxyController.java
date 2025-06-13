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

    @RequestMapping({"/proxy", "/proxy/*", "/proxy/*/{id}"})
    public DeferredResult<ResponseEntity<String>> handleRequest(
            HttpServletRequest request,
            //@PathVariable(value = "resourceType", required = false) String resourceType,
            @PathVariable(value = "id", required = false) Optional<String> resourceId,
            @RequestBody(required = false) Optional<String> payload
    ) {

        // Retrieving the resourceType from the json payload
        String resourceType = payload.map(body -> getResourceTypeFromJson(body, "resourceType")).orElse("");

        RequestParams proxyRequestParams = null;
        

        LOGGER.info("Checking resourceType for Bundle");
        LOGGER.info("Resource type from json \"{}\"", resourceType);
        List<Map.Entry<String, RequestParams>> testIds = new ArrayList<>();

        if ("Bundle".equals(resourceType)) {

            // loop through all entries in the bundle
            try {
                JsonNode root = objectMapper.readTree(payload.get());
                JsonNode entries = root.at(this.fhirRefCodes.get("entry").get());
                
                if (entries.isArray()) {
                    for (JsonNode entry : entries) {
                        String method = "";
                        String entryResourceType = "";
                        String vaccineCode = "";
                        
                        // Get resource type for each entry
                        JsonNode resourceNode = entry.get("resource");
                        if (resourceNode != null && resourceNode.has("resourceType")) {
                            entryResourceType = resourceNode.get("resourceType").asText();
                            LOGGER.info("Found entry with resource type: {}", entryResourceType);
                            
                            // Get vaccine code if resource type is Immunization
                            if (resourceNode.has("vaccineCode")) {
                                JsonNode vaccineCodeNode = resourceNode.get("vaccineCode");
                                if (vaccineCodeNode.has("coding")) {
                                    JsonNode codingArray = vaccineCodeNode.get("coding");
                                    if (codingArray.isArray() && codingArray.size() > 0) {
                                        JsonNode firstCoding = codingArray.get(0);
                                        if (firstCoding.has("code")) {
                                            vaccineCode = firstCoding.get("code").asText();
                                            LOGGER.info("Found vaccine code: {}", vaccineCode);
                                        }
                                    }
                                }
                            }
                        }

                        // Get request method and URL for each entry
                        JsonNode requestNode = entry.get("request");
                        if (requestNode != null) {
                            if (requestNode.has("method")) {
                                method = requestNode.get("method").asText().toLowerCase();
                                LOGGER.info("Found request method: {}", method);
                            }
                        }

                        testIds.add(new AbstractMap.SimpleEntry<>(method + "-" + entryResourceType + "-" + vaccineCode, fhirProxyServiceHelper.toFhirHttpParams(request, "", Optional.of(resourceNode.toString()))));
                        LOGGER.info("Test ID: {}", method + "-" + entryResourceType + "-" + vaccineCode);
                    }
                }
            } catch (JsonProcessingException e) {
                LOGGER.warn("Failed to parse Bundle entries: {}", e.getMessage());
            }

            proxyRequestParams = fhirProxyServiceHelper.toFhirHttpParams(request, "", payload);


        } else {
            Optional<String> referenceCode = payload.flatMap(body -> getReferenceCode(body, resourceType));

            String fullPath = String.format("%s%s", resourceType, resourceId.map(value -> "/" + value).orElse(""));
            proxyRequestParams = fhirProxyServiceHelper.toFhirHttpParams(request, fullPath, payload);

            testIds.add(new AbstractMap.SimpleEntry<>(String.format("%s-%s%s",
                    proxyRequestParams.method().toString().toLowerCase(),
                    resourceType.replace("/", ""),
                    referenceCode.map(code -> "-" + code).orElse("")), proxyRequestParams));
        }


        

        LOGGER.info("Starting test session(s) for \"{}\"", testIds);
        // forwards the request to the FHIR ACC server.
        var deferredResult = new DeferredResult<ResponseEntity<String>>();
        var deferredRequest = new DeferredRequest(proxyRequestParams, deferredResult);

        for (Entry<String, RequestParams> testId : testIds) {
            try {
                // start test sessions and defer the request
                var startSessionPayload = StartSessionRequestPayload.fromRequestParams(new String[]{testId.getKey()}, testId.getValue());
                var itbResponse = itbRestClient.startSession(startSessionPayload);
                var createdSessions = itbResponse.createdSessions();
                var sessionId = createdSessions[0].session();

                LOGGER.info("Test session(s) created: {}", (Object[]) createdSessions);
                deferredRequests.put(sessionId, deferredRequest);

            } catch (Exception e) {
                LOGGER.warn("Failed to start test session(s) for testId {}: {}", testId, e.getMessage());
                deferredRequest.resolve();
                

                //          In case of failure trigger the general suite to check for issues.
                //          No code included, method - resourceType only
                try {
                    String generalTestId = String.format("%s-%s",
                            proxyRequestParams.method().toString().toLowerCase(),
                            resourceType.replace("/", "")
                    );

                    LOGGER.info("Initiating general test session(s), testId:" + testId.getKey().replaceAll("-[^-]*$", ""));

                    var startSessionPayload = StartSessionRequestPayload.fromRequestParams(new String[]{testId.getKey().replaceAll("-[^-]*$", "")}, testId.getValue());
                    var itbResponse = itbRestClient.startSession(startSessionPayload);
                    var createdSessions = itbResponse.createdSessions();
                    var sessionId = createdSessions[0].session();

                    LOGGER.info("Test session(s) created: {}", (Object[]) createdSessions);
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
