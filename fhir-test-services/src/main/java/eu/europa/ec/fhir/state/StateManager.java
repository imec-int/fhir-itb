package eu.europa.ec.fhir.state;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.gitb.core.LogLevel;
import com.gitb.tr.TAR;
import com.gitb.tr.TestResultType;
import eu.europa.ec.fhir.gitb.TestBedNotifier;
import eu.europa.ec.fhir.handlers.FhirClient;
import eu.europa.ec.fhir.handlers.RequestResult;
import eu.europa.ec.fhir.utils.Utils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Component used to manage the ongoing state of test sessions.
 */
@Component
public class StateManager {

    private static final Logger LOG = LoggerFactory.getLogger(StateManager.class);
    private static final String POST_TO_VALIDATE = "postToValidate";

    private final JsonPointer patientPointer = JsonPointer.compile("/patient/reference");

    @Autowired
    private TestBedNotifier testBedNotifier;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private Utils utils;
    @Autowired
    private FhirClient fhirClient;

    /**
     * Map that represents the ongoing test sessions' state. This is not defined as synchronised given that we use
     * synchronised guards in all public methods.
     */
    private final Map<String, Map<String, Object>> testSessions = new HashMap<>();
    /**
     * Map that represents saved POSTs for patients that were received without a test session expecting them.
     */
    private final Map<String, List<SavedPost>> savedPosts = new HashMap<>();
    private String fhirServerEndpoint = null;
    private final Object lock = new Object();

    /**
     * Record a new test session identifier.
     *
     * @param testSessionIdentifier The test session identifier.
     */
    public void recordSession(String testSessionIdentifier) {
        synchronized (lock) {
            testSessions.put(testSessionIdentifier, new HashMap<>());
        }
    }

    /**
     * Destroy all state relevant to a given test session.
     *
     * @param testSessionIdentifier The test session identifier.
     */
    public void destroySession(String testSessionIdentifier) {
        synchronized (lock) {
            testSessions.remove(testSessionIdentifier);
            if (testSessions.isEmpty()) {
                // If we have no test sessions ongoing clear also all other state.
                savedPosts.clear();
            }
        }
    }

    /**
     * Record a new test session identifier.
     *
     * @param fhirServerEndpoint The base API endpoint for the embedded FHIR server.
     */
    public void recordConfiguration(String fhirServerEndpoint) {
        this.fhirServerEndpoint = fhirServerEndpoint;
    }

    /**
     * Record an expected POST by a FHIR client.
     * <p/>
     * We record these as an ArrayList in case we may have a test session expecting multiple POSTs in parallel.
     *
     * @param expected The expected POST's information.
     */
    public void recordExpectedPost(ExpectedPost expected) {
        synchronized (lock) {
            // Check to see if the expected POST has already been received.
            if (savedPosts.containsKey(expected.patient())) {
                // We already have POSTs for the requested patient.
                List<SavedPost> posts = savedPosts.get(expected.patient());
                if (posts != null && !posts.isEmpty()) {
                    LOG.info("A POST for patient [{}] was already received. Notifying test session [{}] to proceed.", expected.patient(), expected.testSessionId());
                    SavedPost post = posts.remove(0);
                    completeExpectedPost(expected, post.body(), post.serverResult());
                    if (posts.isEmpty()) {
                        savedPosts.remove(expected.patient());
                    }
                }
            } else {
                // No already received POST found - park this expectation for future checks.
                var sessionState = testSessions.get(expected.testSessionId());
                if (sessionState != null) {
                    var expectedPosts = (ArrayList<ExpectedPost>) sessionState.computeIfAbsent(POST_TO_VALIDATE, (key) -> new ArrayList<ExpectedPost>());
                    expectedPosts.add(expected);
                    // Add also a message to the test session's log.
                    addToSessionLog(expected.testSessionId(), expected.callbackAddress(), LogLevel.INFO, "Expecting to receive post for patient [%s].".formatted(expected.patient()));
                }
            }
        }
    }

    /**
     * Construct the final URI to use for calling our internal FHIR server.
     *
     * @param base The base URI.
     * @param pathExtension The extension.
     * @return The full URI to use.
     */
    private String constructUri(String base, String pathExtension) {
        return StringUtils.appendIfMissing(base, "/") + StringUtils.removeStart(pathExtension, "/");
    }

    /**
     * Extract the patient reference from the provided payload.
     *
     * @param body The body to extract from.
     * @return The located patient reference.
     */
    private Optional<String> extractPatient(String body) {
        String patient = null;
        try {
            JsonNode payload = objectMapper.readTree(body);
            JsonNode patientNode = payload.at(patientPointer);
            if (patientNode instanceof TextNode) {
                patient = patientNode.asText();
            }
        } catch (JsonProcessingException e) {
            LOG.warn("Failed to parse body as JSON", e);
        }
        return Optional.ofNullable(patient);
    }

    /**
     * Handle a received POST on the exposed FHIR server API.
     * <p/>
     * Note that we need some reference information to determine whether an incoming POST is relevant for a given test session.
     * For this purpose in the current implementation we use the patient reference, with the assumption (even if not
     * implemented in test cases and test data currently), that each test session would expect the creation of a new
     * patient and then use her reference to distinguish between concurrent test sessions.
     *
     * @param pathExtension The path extension following the base endpoint.
     * @param body The POST's body.
     * @return The response to return to the caller.
     */
    public Optional<RequestResult> handleReceivedPost(String pathExtension, String body) {
        Optional<RequestResult> result = Optional.empty();
        if (fhirServerEndpoint != null) {
            Optional<String> patient = extractPatient(body);
            if (patient.isPresent()) {
                body = utils.prettyPrintJson(body);
                // Call embedded FHIR server.
                RequestResult serverResult = fhirClient.callServer(HttpMethod.POST, constructUri(fhirServerEndpoint, pathExtension), body);
                result = Optional.of(serverResult);
                // Check to see if any test sessions were expecting the call.
                synchronized (lock) {
                    boolean pendingSessionFound = false;
                    for (var sessionEntry: testSessions.entrySet()) {
                        List<ExpectedPost> expectedPosts = (List<ExpectedPost>) sessionEntry.getValue().get(POST_TO_VALIDATE);
                        if (expectedPosts != null) {
                            int index = 0;
                            for (var expectedPost: expectedPosts) {
                                if (expectedPost.patient().equals(patient.get())) {
                                    // Found a pending test session.
                                    pendingSessionFound = true;
                                    // Signal to the Test Bed that the expected POST is completed so that the relevant test session continues.
                                    completeExpectedPost(expectedPost, body, serverResult);
                                    break;
                                }
                                index += 1;
                            }
                            if (pendingSessionFound) {
                                expectedPosts.remove(index);
                                break;
                            }
                        }
                    }
                    if (pendingSessionFound) {
                        LOG.info("Received expected POST for patient reference [%s]".formatted(patient));
                    } else {
                        // No pending session was matched - park the request's info.
                        LOG.info("Received POST for patient reference [%s] that was not yet expected by a test session".formatted(patient));
                        var savedPostsForPatient = savedPosts.computeIfAbsent(patient.get(), (key) -> new ArrayList<>());
                        savedPostsForPatient.add(new SavedPost(patient.get(), body, serverResult));
                    }
                }
            } else {
                LOG.warn("Received POST without a patient reference.");
            }
        } else {
            LOG.warn("Received POST without having the embedded FHIR server endpoint set.");
        }
        return result;
    }

    /**
     * Complete an expected POST for a patient by creating the relevant report and reporting back to the Test Bed.
     *
     * @param expectedPost The expected POST's information.
     * @param payload The received payload.
     * @param result The result of the call once forwarded to our internal FHIR server.
     */
    private void completeExpectedPost(ExpectedPost expectedPost, String payload, RequestResult result) {
        TAR report = utils.createReport(TestResultType.SUCCESS);
        utils.addCommonReportData(report, null, payload, result);
        testBedNotifier.notifyTestBed(expectedPost.testSessionId(), expectedPost.callId(), expectedPost.callbackAddress(), report);
    }

    /**
     * Add an entry to the test session log.
     *
     * @param sessionId The test session to log this for.
     * @param callbackAddress The callback address on which log entries are to be signalled (this matches the messaging
     *                        callback address).
     * @param level The log level.
     * @param message The message.
     */
    private void addToSessionLog(String sessionId, String callbackAddress, LogLevel level, String message) {
        // Log first in our own log.
        switch (level) {
            case INFO -> LOG.info(message);
            case WARNING -> LOG.warn(message);
            case ERROR -> LOG.error(message);
            default -> LOG.debug(message);
        }
        // Log also in the test session log.
        testBedNotifier.sendLogMessage(sessionId, callbackAddress, message, level);
    }

}
