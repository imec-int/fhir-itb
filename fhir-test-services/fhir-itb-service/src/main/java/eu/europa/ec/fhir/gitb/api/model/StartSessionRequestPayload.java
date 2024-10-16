package eu.europa.ec.fhir.gitb.api.model;

import eu.europa.ec.fhir.http.HttpParams;
import eu.europa.ec.fhir.utils.ITBUtils;

public record StartSessionRequestPayload(String[] testCase, InputMapping[] inputMapping) {
    public static StartSessionRequestPayload fromRequestParams(String[] testCase, HttpParams httpParams) {
        var requestInputMapping = new InputMapping();
        var requestInput = requestInputMapping.input();
        requestInput.setName("request");
        requestInput.setType("map");

        var requestInputContent = requestInput.getItem();
        requestInputContent.add(ITBUtils.createAnyContent("uri", httpParams.uri().toString()));
        requestInputContent.add(ITBUtils.createAnyContent("headers", httpParams.headers().toString()));
        requestInputContent.add(ITBUtils.createAnyContent("body", httpParams.body()));
        requestInputContent.add(ITBUtils.createAnyContent("method", httpParams.method().toString()));

        return new StartSessionRequestPayload(testCase, new InputMapping[]{requestInputMapping});
    }
}
