package eu.europa.ec.fhir.handlers;

import eu.europa.ec.fhir.FhirProxyService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * Proxies requests to the configured FHIR server while performing additional processing.
 */
@RestController
public class FhirProxyController {

    private static final Logger LOGGER = LoggerFactory.getLogger(FhirProxyController.class);

    private final FhirProxyService fhirProxyService;

    public FhirProxyController(FhirProxyService fhirProxyService) {
        this.fhirProxyService = fhirProxyService;
    }

    @RequestMapping(value = "/proxy/{path}")
    public ResponseEntity<String> handleRequest(
            HttpServletRequest request,
            @PathVariable String path
    ) throws IOException, InterruptedException {
        LOGGER.info("Proxying request to FHIR server: {}", path);

        var fhirResponse = fhirProxyService.proxyRequest(request, path);

        // TODO: trigger automated tests against the response

        return fhirResponse;
    }
}
