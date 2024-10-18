package eu.europa.ec.fhir.gitb;

import eu.europa.ec.fhir.proxy.DeferredSupplier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Optional;

/**
 * Maps test session IDs to {@link DeferredSupplier}s of {@link ResponseEntity}s.
 */
@Service
public class DeferredRequestMapper {

    private final HashMap<String, DeferredSupplier<ResponseEntity<String>>> deferredMap = new HashMap<>();

    public void put(String key, DeferredSupplier<ResponseEntity<String>> deferredSupplier) {
        deferredMap.put(key, deferredSupplier);
    }

    public Optional<DeferredSupplier<ResponseEntity<String>>> get(String key) {
        return Optional.ofNullable(deferredMap.get(key));
    }

    public void remove(String key) {
        deferredMap.remove(key);
    }
}
