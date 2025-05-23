package eu.europa.ec.fhir.gitb.api.model;

import eu.europa.ec.fhir.http.RequestParams;
import eu.europa.ec.fhir.utils.ITBUtils;

public record StartSessionRequestPayload(String[] testCase, InputMapping[] inputMapping) {
    public static StartSessionRequestPayload fromRequestParams(String[] testCase, RequestParams requestParams) {
        var requestInputMapping = new InputMapping();
        var content = requestInputMapping.input();
        content.setName("request");
        content.setType("map");

        var items = content.getItem();
        items.add(ITBUtils.createAnyContent("uri", requestParams.uri().toString()));
        items.add(ITBUtils.createAnyContent("headers", requestParams.headers().toString()));
        items.add(ITBUtils.createAnyContent("method", requestParams.method().toString()));
        requestParams.body().ifPresent(body -> items.add(ITBUtils.createAnyContent("body", body)));

        return new StartSessionRequestPayload(testCase, new InputMapping[]{requestInputMapping});
    }
}
