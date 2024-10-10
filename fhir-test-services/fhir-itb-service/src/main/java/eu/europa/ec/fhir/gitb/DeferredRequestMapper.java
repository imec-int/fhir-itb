package eu.europa.ec.fhir.gitb;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.HashMap;
import java.util.Optional;

@Service
public class DeferredRequestMapper {

    private final HashMap<String, DeferredResult<ResponseEntity<?>>> deferredRequests = new HashMap<>();

    public void putDeferredRequest(String key, DeferredResult<ResponseEntity<?>> deferredResult) {
        deferredRequests.put(key, deferredResult);
    }

    public Optional<DeferredResult<ResponseEntity<?>>> getDeferredRequest(String testId) {
        return Optional.ofNullable(deferredRequests.get(testId));
    }
}
