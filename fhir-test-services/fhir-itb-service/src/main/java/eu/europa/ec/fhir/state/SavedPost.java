package eu.europa.ec.fhir.state;

import eu.europa.ec.fhir.handlers.RequestResult;

/**
 * Information on a received POST for a patient that was not immediately matched to a test session.
 *
 * @param patient The patient reference.
 * @param body The POST body.
 * @param serverResult The result returned to the caller (produced from the embedded FHIR server).
 */
public record SavedPost(String patient, String body, RequestResult serverResult) {
}
