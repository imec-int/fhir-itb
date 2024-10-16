package eu.europa.ec.fhir.handlers;

import eu.europa.ec.fhir.gitb.DeferredRequestMapper;
import eu.europa.ec.fhir.gitb.api.model.StartSessionRequestPayload;
import eu.europa.ec.fhir.http.HttpParams;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.function.Supplier;

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
        HttpParams requestParams = fhirProxyService.getFhirHttpParams(request, path, body);
        String testId = String.format("%s%s", requestParams.method().toString().toLowerCase(), path.replace("/", "-"));

        LOGGER.debug("Starting test session(s) for \"{}\"", testId);

        var deferredResult = new DeferredResult<ResponseEntity<String>>();
        Supplier<ResponseEntity<String>> deferred = () -> {
            // proxy the request
            var spec = restClient
                    .method(requestParams.method())
                    .uri(requestParams.uri())
                    .headers(headers -> headers.addAll(requestParams.headers()));

            if (requestParams.body() != null) {
                spec.body(requestParams.body());
            }

            var response = spec.retrieve()
                    // don't care about the result, just forward the response
                    .onStatus(status -> true, (req, res) -> {})
                    .toEntity(String.class);

            deferredResult.setResult(response);
            return response;
        };

        try {
            // start test sessions and defer the request
            var startSessionPayload = StartSessionRequestPayload.fromRequestParams(new String[]{testId}, requestParams);
            var itbResponse = itbRestClient.startSession(startSessionPayload);
            var createdSessions = itbResponse.createdSessions();
            var sessionId = createdSessions[0].session();

            LOGGER.info("Test session(s) {} created!", (Object[]) createdSessions);
            deferredRequests.put(sessionId, deferred);
        } catch (Exception e) {
            LOGGER.warn("Failed to start test session(s): {}", e.getMessage());
            deferred.get();
        }

        return deferredResult;
    }
}
