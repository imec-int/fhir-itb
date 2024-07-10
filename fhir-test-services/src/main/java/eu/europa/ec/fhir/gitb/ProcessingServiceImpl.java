package eu.europa.ec.fhir.gitb;

import com.gitb.ps.Void;
import com.gitb.ps.*;
import com.gitb.tr.TestResultType;
import eu.europa.ec.fhir.state.StateManager;
import eu.europa.ec.fhir.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import eu.europa.ec.fhir.handlers.PseudonymizationHandler;
import com.gitb.core.ValueEmbeddingEnumeration;

/**
 * Implementation of the GITB messaging API to handle messaging calls.
 */
@Component
public class ProcessingServiceImpl implements ProcessingService {

    @Autowired
    private StateManager stateManager;
    @Autowired
    private Utils utils;
    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(MessagingServiceImpl.class);

    @Override
    public GetModuleDefinitionResponse getModuleDefinition(Void aVoid) {
        // Empty implementation.
        return new GetModuleDefinitionResponse();
    }

    /**
     * Called when a "process" step is executed.
     * <p/>
     * This is used currently to pass configuration values to the service when the test session starts.
     *
     * @param processRequest The request.
     * @return The step report.
     */
    @Override
    public ProcessResponse process(ProcessRequest processRequest) {
        String operation = processRequest.getOperation();
        var response = new ProcessResponse();
        if ("init".equals(operation)) {
            stateManager.recordConfiguration(utils.getRequiredString(processRequest.getInput(), "endpoint"));
        }
        if ("pseudonymisation".equals(operation)) {
            // Get the expected inputs.
            var ssin = utils.getRequiredString(processRequest.getInput(), "SSIN");
            var configFilePath = utils.getRequiredString(processRequest.getInput(), "configFilePath");
            //var configFilePath = "resources/config.properties";
            LOG.info(String.format("Received SSIN info (from test case): [%s]" , ssin));
            LOG.info(String.format("Received config file path (from test case): [%s].", configFilePath));
            String pseudominizedPatient;
            // call pseudominization handler to generate pseudonym
            if (!ssin.isBlank()) {
                LOG.info(String.format("Pseudonymisation operation started for SSIN number: [%s].", ssin));
                pseudominizedPatient =  new PseudonymizationHandler().pseudoGenerator(configFilePath, ssin);
            } else {
                LOG.info("Pseudonymisation operation started for default SSIN number: \"84072536717\".");
                pseudominizedPatient =  new PseudonymizationHandler().pseudoGenerator(configFilePath, ssin);
            }

            // Produce the resulting report.
            response.getOutput().add(utils.createAnyContentSimple("result",pseudominizedPatient, ValueEmbeddingEnumeration.STRING));
        }

        response.setReport(utils.createReport(TestResultType.SUCCESS));
        return response;
    }

    @Override
    public BeginTransactionResponse beginTransaction(BeginTransactionRequest beginTransactionRequest) {
        // Empty implementation.
        return new BeginTransactionResponse();
    }

    @Override
    public Void endTransaction(BasicRequest basicRequest) {
        // Empty implementation.
        return new Void();
    }
}
