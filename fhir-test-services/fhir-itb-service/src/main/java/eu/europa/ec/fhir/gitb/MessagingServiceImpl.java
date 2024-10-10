package eu.europa.ec.fhir.gitb;

import com.gitb.ms.BasicRequest;
import com.gitb.ms.BeginTransactionRequest;
import com.gitb.ms.FinalizeRequest;
import com.gitb.ms.GetModuleDefinitionResponse;
import com.gitb.ms.InitiateRequest;
import com.gitb.ms.InitiateResponse;
import com.gitb.ms.MessagingService;
import com.gitb.ms.ReceiveRequest;
import com.gitb.ms.SendRequest;
import com.gitb.ms.SendResponse;
import com.gitb.ms.Void;
import com.gitb.tr.TestResultType;
import eu.europa.ec.fhir.handlers.FhirClient;
import eu.europa.ec.fhir.handlers.RequestResult;
import eu.europa.ec.fhir.state.ExpectedPost;
import eu.europa.ec.fhir.state.StateManager;
import eu.europa.ec.fhir.utils.ITBUtils;
import jakarta.annotation.Resource;
import jakarta.xml.ws.WebServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.net.URI;


/**
 * Implementation of the GITB messaging API to handle messaging calls.
 */
@Component
public class MessagingServiceImpl implements MessagingService {

    /**
     * Logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(MessagingServiceImpl.class);

    @Resource
    private WebServiceContext wsContext;
    @Autowired
    private StateManager stateManager;
    @Autowired
    private FhirClient fhirClient;

    private final DeferredRequestMapper deferredRequestMapper;

    public MessagingServiceImpl(DeferredRequestMapper deferredRequestMapper) {
        this.deferredRequestMapper = deferredRequestMapper;
    }

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
        var sessionId = ITBUtils.getTestSessionIdFromHeaders(wsContext)
                .orElseThrow();
        LOGGER.info("Initiating new test session [{}].", sessionId);
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
        LOGGER.info("Called 'send' from test session [{}].", sendRequest.getSessionId());
        SendResponse response = new SendResponse();

        var input = SendInput.fromRequest(sendRequest);
        var uri = URI.create(input.endpoint());

        RequestResult result = fhirClient.callServer(input.method(), uri, input.payload(), input.token(), input.patientIdentifier());
        var report = ITBUtils.createReport(TestResultType.SUCCESS);
        ITBUtils.addCommonReportData(report, input.endpoint(), input.payload(), result);
        response.setReport(report);

        var key = String.format("%s%s", input.method().toString()
                .toLowerCase(), uri.getPath().replace("/", "-"));

        deferredRequestMapper.getDeferredRequest(key)
                .ifPresent(deferredRequest -> {
                    LOGGER.info("Found deferred request for key [{}]", key);
                    HttpHeaders headers = new HttpHeaders();
                    result.headers().map().forEach((headerName, values) -> {
                        for (String value : values) {
                            headers.add(headerName, value);
                        }
                    });
                    deferredRequest.setResult(new ResponseEntity<>(result.body(), headers, HttpStatusCode.valueOf(result.status())));
                });

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
        LOGGER.info("Called 'receive' from test session [{}].", receiveRequest.getSessionId());
        var type = ITBUtils.getRequiredString(receiveRequest.getInput(), "type");


        if ("postToValidate".equals(type)) {
            var expectedPatient = ITBUtils.getRequiredString(receiveRequest.getInput(), "patient");
            LOGGER.info(String.format("Received patient info (from test case): [{%s}]: ", expectedPatient));
            stateManager.recordExpectedPost(new ExpectedPost(
                    receiveRequest.getSessionId(),
                    // The call ID distinguishes the specific "receive" step that triggered this. This is useful if we have "parallel" receive steps to distinguish between them.
                    receiveRequest.getCallId(),
                    // The callback address extracted here will be used later on to notify the Test Bed.
                    ITBUtils.getReplyToAddressFromHeaders(wsContext)
                            .orElseThrow(),
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
        LOGGER.info("Finalising test session [{}].", finalizeRequest.getSessionId());
        stateManager.destroySession(finalizeRequest.getSessionId());
        return new Void();
    }

}
