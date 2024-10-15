package eu.europa.ec.fhir.handlers;

import eu.europa.ec.fhir.gitb.DeferredExchangeMapper;
import eu.europa.ec.fhir.handlers.ItbRestClient.InputMapping;
import eu.europa.ec.fhir.handlers.ItbRestClient.StartSessionRequestPayload;
import eu.europa.ec.fhir.proxy.DeferredExchange;
import eu.europa.ec.fhir.utils.ITBUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

/**
 * Triggers test runs based on request parameters.
 */
@RestController
public class FhirProxyController {

    private static final Logger LOGGER = LoggerFactory.getLogger(FhirProxyController.class);

    private final ItbRestClient itbRestClient;
    private final FhirProxyService fhirProxyService;
    private final DeferredExchangeMapper deferredExchangeMapper;

    public FhirProxyController(ItbRestClient itbRestClient, FhirProxyService fhirProxyService, DeferredExchangeMapper deferredExchangeMapper) {
        this.itbRestClient = itbRestClient;
        this.fhirProxyService = fhirProxyService;
        this.deferredExchangeMapper = deferredExchangeMapper;
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

        // TODO: pass the input the same way that the response is passed (a single object with all the necessary fields)
        var requestBodyInput = ITBUtils.createAnyContent("requestBody", body);
        var requestTokenInput = ITBUtils.createAnyContent("requestToken", token);
        var requestMethodInput = ITBUtils.createAnyContent("requestMethod", requestMethod);

        var startSessionPayload = new StartSessionRequestPayload(
                new String[]{testId},
                new InputMapping[]{
                        new InputMapping(requestBodyInput),
                        new InputMapping(requestTokenInput),
                        new InputMapping(requestMethodInput)
                }
        );

        var deferred = new DeferredExchange(fhirProxyService.buildRequest(request, path, body));
        try {
            var itbResponse = itbRestClient.startSession(startSessionPayload);
            var sessionId = itbResponse.createdSessions()[0].session();
            LOGGER.info("Test session(s) {} created!", (Object[]) itbResponse.createdSessions());
            deferredExchangeMapper.put(sessionId, deferred);
        } catch (Exception e) {
            LOGGER.warn("Failed to start test session(s): {}", e.getMessage());
            // if the tests cannot run, perform the exchange directly
            deferred.exchange();
        }

        return deferred;
    }
}
