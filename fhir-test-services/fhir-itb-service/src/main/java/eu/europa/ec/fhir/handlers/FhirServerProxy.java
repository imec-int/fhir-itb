package eu.europa.ec.fhir.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Proxies all requests as they come to the configured FHIR server and
 * runs automated tests against their responses.
 */
@RestController
public class FhirServerProxy {

    private static final Logger LOGGER = LoggerFactory.getLogger(FhirServerProxy.class);

    public record ProxyResponse (String body, String path) {}

    @RequestMapping(value= "/proxy/{path}")
    public ResponseEntity<ProxyResponse> handleRequest(@PathVariable String path, @RequestBody final String body) {
        LOGGER.info("Proxying request to FHIR server: {}", path);
        // TODO: read configuration
        // TODO: forward requests to the FHIR server
        // TODO: trigger automated tests against the responses (don't wait for this)
        // TODO: return the original response
        return new ResponseEntity<>(new ProxyResponse(body, path), HttpStatus.OK);
    }
}
