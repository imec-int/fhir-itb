package eu.europa.ec.fhir.gitb;

import eu.europa.ec.fhir.proxy.DeferredExchange;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Optional;

@Service
public class DeferredExchangeMapper {

    private final HashMap<String, DeferredExchange> deferredMap = new HashMap<>();

    public void put(String key, DeferredExchange deferredExchange) {
        deferredMap.put(key, deferredExchange);
    }

    public Optional<DeferredExchange> get(String key) {
        return Optional.ofNullable(deferredMap.get(key));
    }

    public Optional<DeferredExchange> remove(String key) {
        return Optional.ofNullable(deferredMap.remove(key));
    }
}
