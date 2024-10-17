package eu.europa.ec.fhir.gitb.api.model;

import eu.europa.ec.fhir.http.HttpParams;
import eu.europa.ec.fhir.utils.ITBUtils;

public record StartSessionRequestPayload(String[] testCase, InputMapping[] inputMapping) {
    public static StartSessionRequestPayload fromRequestParams(String[] testCase, HttpParams httpParams) {
        var requestInputMapping = new InputMapping();
        var content = requestInputMapping.input();
        content.setName("request");
        content.setType("map");

        var items = content.getItem();
        items.add(ITBUtils.createAnyContent("uri", httpParams.uri().toString()));
        items.add(ITBUtils.createAnyContent("headers", httpParams.headers().toString()));
        items.add(ITBUtils.createAnyContent("body", httpParams.body()));
        items.add(ITBUtils.createAnyContent("method", httpParams.method().toString()));

        return new StartSessionRequestPayload(testCase, new InputMapping[]{requestInputMapping});
    }
}
