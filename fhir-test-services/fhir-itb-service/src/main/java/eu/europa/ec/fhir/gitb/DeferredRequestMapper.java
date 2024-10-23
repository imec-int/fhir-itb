package eu.europa.ec.fhir.gitb;

import eu.europa.ec.fhir.proxy.DeferredRequest;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Optional;

/**
 * Maps test session IDs to {@link DeferredRequest}s.
 */
@Service
public class DeferredRequestMapper {

    private final HashMap<String, DeferredRequest> deferredMap = new HashMap<>();

    public void put(String key, DeferredRequest deferredRequest) {
        deferredMap.put(key, deferredRequest);
    }

    public Optional<DeferredRequest> get(String key) {
        return Optional.ofNullable(deferredMap.get(key));
    }

    public void remove(String key) {
        deferredMap.remove(key);
    }
}
