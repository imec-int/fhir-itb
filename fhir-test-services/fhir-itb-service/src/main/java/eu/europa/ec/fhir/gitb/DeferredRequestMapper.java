package eu.europa.ec.fhir.gitb;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Maps test session IDs to {@link Supplier<ResponseEntity>}.
 */
@Service
public class DeferredRequestMapper {

    private final HashMap<String, Supplier<ResponseEntity<String>>> deferredMap = new HashMap<>();

    public void put(String key, Supplier<ResponseEntity<String>> deferredRequest) {
        deferredMap.put(key, deferredRequest);
    }

    public Optional<Supplier<ResponseEntity<String>>> get(String key) {
        return Optional.ofNullable(deferredMap.get(key));
    }

    public void remove(String key) {
        deferredMap.remove(key);
    }
}
