package eu.europa.ec.fhir.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

/**
 * Proxies requests to the configured FHIR server while performing additional processing.
 */
@RestController
public class FhirProxyController {

    private static final Logger LOGGER = LoggerFactory.getLogger(FhirProxyController.class);

    /**
     * A single system might have a different role in different specifications and
     * will therefore have a different Actor ID per specification.
     * By mapping endpoints to an Actor IDs, we can effectively link them to specifications.
     * TODO: load from configuration
     */
    private final Map<String, String> ACTOR_BY_PATH = Map.of(
            "AllergyIntolerance", "1AAFC15FX5961X4334X860BXD0B0F7428CD2"
            // e.g.: "some/other/path", "actor-id-of-system-in-spec"
    );

    /**
     * Currently there is only one vendor, so we hardcode the pre-configured system ID.
     * TODO: read from configuration
     */
    private Optional<String> resolveSystemId(String token) {
        return Optional.of("F3F3D983X081DX40D0XA2C1XC25D1CBA430C");
    }

    /**
     * Returns the Actor ID of the SUT based on the given endpoint.
     */
    private Optional<String> resolveActorId(String path) {
        return Optional.ofNullable(ACTOR_BY_PATH.get(path));
    }

    @RequestMapping(value = "/proxy/{path}")
    public ResponseEntity<String> handleRequest(
            @RequestBody String body,
            @RequestHeader("Authorization") String token,
            @PathVariable String path
    ) {
        var system_id = resolveSystemId(token);
        if (system_id.isEmpty()) {
            LOGGER.warn("System not found");
            return ResponseEntity.badRequest().build();
        }

        var actor_id = resolveActorId(path);
        if (actor_id.isEmpty()) {
            LOGGER.warn("Actor not found");
            return ResponseEntity.badRequest().build();
        }

        // TODO: trigger the correct test and pass the body and token

        return ResponseEntity.accepted().build();
    }
}
