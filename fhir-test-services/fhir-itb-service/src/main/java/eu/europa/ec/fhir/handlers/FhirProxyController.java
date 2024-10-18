package eu.europa.ec.fhir.handlers;

import eu.europa.ec.fhir.gitb.DeferredRequestMapper;
import eu.europa.ec.fhir.gitb.api.model.StartSessionRequestPayload;
import eu.europa.ec.fhir.http.RequestParams;
import eu.europa.ec.fhir.proxy.DeferredSupplier;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.async.DeferredResult;

/**
 * Triggers test runs based on request parameters.
 */
@RestController
public class FhirProxyController {

    private static final Logger LOGGER = LoggerFactory.getLogger(FhirProxyController.class);

    private final ItbRestClient itbRestClient;
    private final FhirProxyService fhirProxyService;
    private final DeferredRequestMapper deferredRequests;
    private final RestClient restClient;

    public FhirProxyController(ItbRestClient itbRestClient, FhirProxyService fhirProxyService, DeferredRequestMapper deferredRequests, RestClient restClient) {
        this.itbRestClient = itbRestClient;
        this.fhirProxyService = fhirProxyService;
        this.deferredRequests = deferredRequests;
        this.restClient = restClient;
    }

    @RequestMapping(value = "/proxy/{*path}")
    public DeferredResult<ResponseEntity<String>> handleRequest(
            HttpServletRequest request,
            @PathVariable("path") String path,
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody(required = false) String body
    ) {
        RequestParams proxyRequestParams = fhirProxyService.getFhirHttpParams(request, path, body);
        String testId = String.format("%s%s", proxyRequestParams.method().toString().toLowerCase(), path.replace("/", "-"));

        LOGGER.debug("Starting test session(s) for \"{}\"", testId);

        var deferredResult = new DeferredResult<ResponseEntity<String>>();
        var deferredRequest = new DeferredSupplier<>(deferredResult, () -> {
            // proxy the request
            var spec = restClient
                    .method(proxyRequestParams.method())
                    .uri(proxyRequestParams.uri())
                    .headers(headers -> headers.addAll(proxyRequestParams.headers()));

            if (proxyRequestParams.body() != null) {
                spec.body(proxyRequestParams.body());
            }

            try {
                return spec.retrieve()
                        // don't care about the result, just forward the response
                        .onStatus(status -> true, (req, res) -> {})
                        .toEntity(String.class);
            } catch (Exception e) {
                LOGGER.warn("Failed to proxy request: {}", e.getMessage());
                return ResponseEntity.status(500).body("Failed to proxy request");
            }
        });

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
            deferredRequest.get();
        }

        return deferredResult;
    }
}
