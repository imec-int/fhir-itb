package eu.europa.ec.fhir.gitb.api.model;

public record SessionInfo(
        String testSuite,
        String testCase,
        String session
) {}
