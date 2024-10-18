package eu.europa.ec.fhir.gitb;

import com.gitb.core.AnyContent;
import com.gitb.ms.Void;
import com.gitb.ms.*;
import com.gitb.tr.TAR;
import com.gitb.tr.TestResultType;
import eu.europa.ec.fhir.utils.ITBUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;


/**
 * Implementation of the GITB messaging API to handle messaging calls.
 */
@Component
public class ProxyMessagingService implements MessagingService {

    /**
     * Logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyMessagingService.class);

    private final DeferredRequestMapper deferredRequestMapper;

    public ProxyMessagingService(DeferredRequestMapper deferredRequestMapper) {
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
        var sessionId = sendRequest.getSessionId();
        SendResponse response = new SendResponse();

        var deferredRequest = deferredRequestMapper.get(sessionId);
        if (deferredRequest.isPresent()) {
            LOGGER.info("Found deferred request for key [{}]", sessionId);
            ResponseEntity<String> result = deferredRequest.get().get();

            LOGGER.info("Deferred Request resolved");

            var report = ITBUtils.createReport(TestResultType.SUCCESS);
            var reportContext = report.getContext();
            reportContext.setName("report");
            reportContext.setType("map");

            var responseContent = new AnyContent();
            responseContent.setName("response");
            responseContent.setType("map");

            var responseItems = responseContent.getItem();
            responseItems.add(ITBUtils.createAnyContent("status", result.getStatusCode().toString()));
            responseItems.add(ITBUtils.createAnyContent("headers", result.getHeaders().toString()));
            responseItems.add(ITBUtils.createAnyContent("body", Optional.ofNullable(result.getBody()).map(Object::toString).orElse("")));

            reportContext.getItem().add(responseContent);
            response.setReport(report);
        } else {
            var input = SendInput.fromRequest(sendRequest);
            RequestResult result = fhirClient.callServer(input.method(), URI.create(input.endpoint()), input.payload(), input.token(), null);
            var report = ITBUtils.createReport(TestResultType.SUCCESS);
            ITBUtils.addCommonReportData(report, input.endpoint(), input.payload(), result);
            response.setReport(report);
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
        var sessionId = finalizeRequest.getSessionId();
        LOGGER.info("Finalising test session [{}].", sessionId);
        deferredRequestMapper.remove(sessionId);
        return new Void();
    }

}