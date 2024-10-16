package eu.europa.ec.fhir.gitb.api.model;

import com.gitb.core.AnyContent;

public record InputMapping(AnyContent input) {
    public InputMapping() {
        this(new AnyContent());
    }
}
