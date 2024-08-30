package eu.europa.ec.fhir.gitb;

import com.gitb.core.LogLevel;
import com.gitb.ms.LogRequest;
import com.gitb.ms.MessagingClient;
import com.gitb.ms.NotifyForMessageRequest;
import com.gitb.tr.TAR;
import com.gitb.tr.TestResultType;
import eu.europa.ec.fhir.utils.Utils;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Component used to notify the Test Bed of received queries.
 * <p/>
 * The main reason of defining this as a separate component is to facilitate making these notifications asynchronous
 * (see the notifyTestBed method that is marked as async).
 */
@Component
public class TestBedNotifier {

    private static final Logger LOG = LoggerFactory.getLogger(TestBedNotifier.class);
    private final ConcurrentHashMap<String, MessagingClient> messagingClientCache = new ConcurrentHashMap<>();

    @Autowired
    private Utils utils;

    /**
     * Send a log message to the Test Bed at a given severity level.
     *
     * @param sessionId The session identifier.
     * @param callbackAddress The Test Bed's callback address to use.
     * @param message The log message.
     * @param level The severity level.
     */
    @Async
    public void sendLogMessage(String sessionId, String callbackAddress, String message, LogLevel level) {
        var logRequest = new LogRequest();
        logRequest.setSessionId(sessionId);
        logRequest.setMessage(message);
        logRequest.setLevel(level);
        getMessagingClient(callbackAddress).log(logRequest);
    }

    /**
     * Notify the Test Bed for a given session.
     *
     * @param sessionId The session ID to notify the test bed for.
     * @param callId The 'receive' call ID to notify the Test Bed for.
     * @param report The report to notify the Test Bed with.
     */
    @Async
    public void notifyTestBed(String sessionId, String callId, String callback, TAR report){
        try {
            LOG.info("Notifying Test Bed for session [{}]", sessionId);
            callTestBed(sessionId, callId, report, callback);
        } catch (Exception e) {
            LOG.warn("Error while notifying test bed for session [{}]", sessionId, e);
            callTestBed(sessionId, callId, utils.createReport(TestResultType.FAILURE), callback);
            throw new IllegalStateException(e);
        }
    }

    /**
     * Call the Test Bed to notify it of received communication.
     *
     * @param sessionId The session ID that this notification relates to.
     * @param callId The 'receive' call ID to notify the test bed for.
     * @param report The TAR report to send back.
     * @param callbackAddress The address on which the call is to be made.
     */
    private void callTestBed(String sessionId, String callId, TAR report, String callbackAddress) {
        // Make the call.
        NotifyForMessageRequest request = new NotifyForMessageRequest();
        request.setSessionId(sessionId);
        request.setCallId(callId);
        request.setReport(report);
        getMessagingClient(callbackAddress).notifyForMessage(request);
    }

    /**
     * Get the messaging client to use for the given Test Bed instance.
     *
     * @param callbackAddress The Test Bed's messaging callback address.
     * @return The client.
     */
    private MessagingClient getMessagingClient(String callbackAddress) {
        return messagingClientCache.computeIfAbsent(callbackAddress, (address) -> {
            var proxyFactoryBean = new JaxWsProxyFactoryBean();
            proxyFactoryBean.setServiceClass(MessagingClient.class);
            proxyFactoryBean.setAddress(callbackAddress);
            return (MessagingClient)proxyFactoryBean.create();
        });
    }

}
