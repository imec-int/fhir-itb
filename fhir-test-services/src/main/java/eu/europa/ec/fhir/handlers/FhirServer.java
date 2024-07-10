package eu.europa.ec.fhir.handlers;

import eu.europa.ec.fhir.state.StateManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * Component used to receive REST calls from FHIR clients.
 * <p/>
 * This is not a full implementation of a FHIR server but rather it is used to proxy an internal FHIR server instance
 * while exposing the necessary API to the client. In regard to this proxying, it will only occur if we have active
 * test sessions waiting for such calls.
 */
@RestController
public class FhirServer {

    private static final Logger LOG = LoggerFactory.getLogger(FhirServer.class);

    @Autowired
    private StateManager stateManager;

    /**
     * Receive an allergy intolerance post.
     *
     * @param pathExtension The path part after the base URI.
     * @param body The request's body.
     * @return The response.
     */
    @PostMapping(value="/server/api/{pathExtension}")
    public ResponseEntity<String> receivePost(@PathVariable String pathExtension, @RequestBody final String body) {
        Optional<RequestResult> response = stateManager.handleReceivedPost(pathExtension, body);
        if (response.isPresent()) {
            var builder = ResponseEntity.status(response.get().status());
            var contentTypeHeader = response.get().contentType();
            if (contentTypeHeader.isPresent()) {
                builder = builder.header(HttpHeaders.CONTENT_TYPE, contentTypeHeader.get());
            }
            return builder.body(response.get().body());
        } else {
            LOG.warn("Returning a 'bad request' response.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

}
