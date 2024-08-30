package eu.europa.ec.fhir.gitb;

import com.gitb.ms.Void;
import com.gitb.ms.*;
import com.gitb.tr.TestResultType;
import eu.europa.ec.fhir.handlers.FhirClient;
import eu.europa.ec.fhir.state.ExpectedPost;
import eu.europa.ec.fhir.state.StateManager;
import eu.europa.ec.fhir.utils.Utils;
import jakarta.annotation.Resource;
import jakarta.xml.ws.WebServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;


/**
 * Implementation of the GITB messaging API to handle messaging calls.
 */
@Component
public class MessagingServiceImpl implements MessagingService {

    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(MessagingServiceImpl.class);

    @Resource
    private WebServiceContext wsContext;
    @Autowired
    private StateManager stateManager;
    @Autowired
    private FhirClient fhirClient;
    @Autowired
    private Utils utils;


    /**
     * This method normally returns documentation on how the service is expected to be used. It is meaningful
     * to implement this if this service would be a published utility that other test developers would want to
     * query and reuse. As it is an internal service we can skip this and return an empty implementation.
     *
     * @param aVoid No parameters.
     * @return The result.
     */
    @Override
    public GetModuleDefinitionResponse getModuleDefinition(Void aVoid) {
        return new GetModuleDefinitionResponse();
    }

    /**
     * Called when a new test session is about to start.
     * <p/>
     * A typical task here is to record the session identifier in the list of active sessions (in case the service
     * needs to manage state across calls). This service also returns a unique identifier as part of the response that
     * the Test Bed will signal back in calls taking place within the same test session. If we don't generate and return
     * such an identifier, the Test Bed's own session identifier will be used (this is useful to cross-check potential
     * issues in test sessions).
     *
     * @param initiateRequest The call's request (we typically don't need to process any of its information).
     * @return The response.
     */
    @Override
    public InitiateResponse initiate(InitiateRequest initiateRequest) {
        /*
         * The session identifier is extracted here from the SOAP headers. In subsequent calls to other operations,
         * this identifier will be directly included in the calls' parameters.
         */
        var sessionId = utils.getTestSessionIdFromHeaders(wsContext).orElseThrow();
        LOG.info("Initiating new test session [{}].", sessionId);
        stateManager.recordSession(sessionId);
        return new InitiateResponse();
    }

    /**
     * Called when a "send" step is executed.
     * <p/>
     * This method is expected to retrieve inputs, trigger whatever processing is needed, and return a synchronous report.
     *
     * @param sendRequest The request's parameters.
     * @return The response.
     */
    @Override
    public SendResponse send(SendRequest sendRequest) {
        // We can access the test session ID from the request's parameters.
        LOG.info("Called 'send' from test session [{}].", sendRequest.getSessionId());
        SendResponse response = new SendResponse();
        response.setReport(utils.createReport(TestResultType.SUCCESS));
        var type = utils.getRequiredString(sendRequest.getInput(), "type");
        if ("post".equals(type)) {
            var endpoint = utils.getRequiredString(sendRequest.getInput(), "endpoint");
            var payload = utils.getRequiredString(sendRequest.getInput(), "payload");
            var result = fhirClient.callServer(HttpMethod.POST, endpoint, payload);
            var report = utils.createReport(TestResultType.SUCCESS);
            utils.addCommonReportData(report, endpoint, payload, result);
            response.setReport(report);
        } else if ("delete".equals(type)) {
            var endpoint = utils.getRequiredString(sendRequest.getInput(), "endpoint");
            var result = fhirClient.callServer(HttpMethod.DELETE, endpoint, null);
            var report = utils.createReport(TestResultType.SUCCESS);
            utils.addCommonReportData(report, endpoint, null, result);
            response.setReport(report);
        } else if ("get".equals(type)) {
            var endpoint = utils.getRequiredString(sendRequest.getInput(), "endpoint");
            var result = fhirClient.callServer(HttpMethod.GET, endpoint, null);
            var report = utils.createReport(TestResultType.SUCCESS);
            utils.addCommonReportData(report, endpoint, null, result);
            response.setReport(report);
        } else {
            throw new IllegalArgumentException("Unsupported type [%s] for 'send' operation.".formatted(type));
        }

        return response;
    }

    /**
     * Called when a "receive" step is executed.
     * <p/>
     * We return from this method a synchronous response to the test session, however the
     * actual message for which we will complete the test session's receive step will be
     * received and handled asynchronously. The report for this message will be provided
     * through the Test Bed's callback API that is made available through the reply-to
     * SOAP header.
     *
     * @param receiveRequest The request's parameters.
     * @return An empty response (the eventual response message will come asynchronously).
     */
    @Override
    public Void receive(ReceiveRequest receiveRequest) {
        LOG.info("Called 'receive' from test session [{}].", receiveRequest.getSessionId());
        var type = utils.getRequiredString(receiveRequest.getInput(), "type");
        

        if ("postToValidate".equals(type)) {
            var expectedPatient = utils.getRequiredString(receiveRequest.getInput(), "patient");
            LOG.info(String.format("Received patient info (from test case): [{%s}]: ",expectedPatient));
            stateManager.recordExpectedPost(new ExpectedPost(
                    receiveRequest.getSessionId(),
                    // The call ID distinguishes the specific "receive" step that triggered this. This is useful if we have "parallel" receive steps to distinguish between them.
                    receiveRequest.getCallId(),
                    // The callback address extracted here will be used later on to notify the Test Bed.
                    utils.getReplyToAddressFromHeaders(wsContext).orElseThrow(),
                    expectedPatient
            ));
        } else {
            throw new IllegalArgumentException("Unsupported type [%s] for 'receive' operation.".formatted(type));
        }
        return new Void();
    }

    /**
     * Called when a transaction starts (if we use transactions in our test cases).
     * <p/>
     * As we don't use transactions we can keep this empty.
     *
     * @param beginTransactionRequest The request.
     * @return An empty response.
     */
    @Override
    public Void beginTransaction(BeginTransactionRequest beginTransactionRequest) {
        return new Void();
    }

    /**
     * Called when a transaction ends (if we use transactions in our test cases).
     * <p/>
     * As we don't use transactions we can keep this empty.
     *
     * @param basicRequest The request.
     * @return An empty response.
     */
    @Override
    public Void endTransaction(BasicRequest basicRequest) {
        return new Void();
    }

    /**
     * Called when a test session completes.
     * <p/>
     * This method is useful if you need to maintain any in-memory state for each test session. In our case we clear the
     * state for the current test session.
     *
     * @param finalizeRequest The request.
     * @return An empty response.
     */
    @Override
    public Void finalize(FinalizeRequest finalizeRequest) {
        LOG.info("Finalising test session [{}].", finalizeRequest.getSessionId());
        stateManager.destroySession(finalizeRequest.getSessionId());
        return new Void();
    }

}
