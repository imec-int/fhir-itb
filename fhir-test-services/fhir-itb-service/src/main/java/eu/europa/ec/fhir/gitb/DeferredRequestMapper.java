package eu.europa.ec.fhir.gitb;

import org.springframework.stereotype.Service;
import proxy.DeferredRequest;

import java.util.HashMap;
import java.util.Optional;

@Service
public class DeferredRequestMapper {

    private final HashMap<String, DeferredRequest> deferredRequests = new HashMap<>();

    public void put(String key, DeferredRequest deferredResult) {
        deferredRequests.put(key, deferredResult);
    }

    public Optional<DeferredRequest> get(String testId) {
        return Optional.ofNullable(deferredRequests.get(testId));
    }
}
