package eu.europa.ec.fhir.gitb.api.model;

public record StartSessionResponse(
        SessionInfo[] createdSessions
) {}
