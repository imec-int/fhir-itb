package eu.europa.ec.fhir.proxy;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@ConfigurationProperties(prefix = "fhir.refcode")
public class FhirRefCodes {
    private Map<String, String> paths;

    public void setPaths(Map<String, String> paths) {
        this.paths = Collections.unmodifiableMap(paths);
    }

    public Optional<String> get(String resourceType) {
        return paths.get(resourceType).describeConstable();
    }
}
