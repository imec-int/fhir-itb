package eu.europa.ec.fhir.gitb;

import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.ps.BasicRequest;
import com.gitb.ps.BeginTransactionRequest;
import com.gitb.ps.BeginTransactionResponse;
import com.gitb.ps.GetModuleDefinitionResponse;
import com.gitb.ps.ProcessRequest;
import com.gitb.ps.ProcessResponse;
import com.gitb.ps.ProcessingService;
import com.gitb.ps.Void;
import com.gitb.tr.TestResultType;
import eu.europa.ec.fhir.accesstoken.AccessTokenGenerator;
import eu.europa.ec.fhir.handlers.KarateHandler;
import eu.europa.ec.fhir.handlers.PseudonymizationHandler;
import eu.europa.ec.fhir.state.StateManager;
import eu.europa.ec.fhir.utils.ITBUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Map;

/**
 * Implementation of the GITB messaging API to handle messaging calls.
 */
@Component
public class ProcessingServiceImpl implements ProcessingService {

    @Autowired
    private StateManager stateManager;

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ProcessingServiceImpl.class);

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
            stateManager.recordConfiguration(ITBUtils.getRequiredString(processRequest.getInput(), "endpoint"));
        }
        if ("pseudonymisation".equals(operation)) {
            // Get the expected inputs.
            var ssin = ITBUtils.getRequiredString(processRequest.getInput(), "SSIN");
            var configFilePath = ITBUtils.getRequiredString(processRequest.getInput(), "configFilePath");
            //var configFilePath = "resources/config.properties";
            LOG.info(String.format("Received SSIN info (from test case): [%s]", ssin));
            LOG.info(String.format("Received config file path (from test case): [%s].", configFilePath));
            String pseudominizedPatient;
            // call pseudominization handler to generate pseudonym
            if (!ssin.isBlank()) {
                LOG.info(String.format("Pseudonymisation operation started for SSIN number: [%s].", ssin));
                pseudominizedPatient = new PseudonymizationHandler().pseudoGenerator(configFilePath, ssin);
            } else {
                LOG.info("Pseudonymisation operation started for default SSIN number from the configuration file");
                pseudominizedPatient = new PseudonymizationHandler().pseudoGenerator(configFilePath, ssin);
            }

            // Produce the resulting report.
            response.getOutput()
                    .add(ITBUtils.createAnyContentSimple("result", pseudominizedPatient, ValueEmbeddingEnumeration.STRING));
        }

        if ("authentication".equals(operation)) {
            // Get the expected inputs.
            var configFilePath = ITBUtils.getRequiredString(processRequest.getInput(), "configFilePath");
            LOG.info(String.format("Received config file path (from test case) for authentication: [%s].", configFilePath));
            //call access token generator
            String accessToken = new AccessTokenGenerator().generateAccessToken(new File(configFilePath));
            LOG.info(String.format("generated access token: [%s].", accessToken));
            // Produce the resulting report.
            response.getOutput()
                    .add(ITBUtils.createAnyContentSimple("result", accessToken, ValueEmbeddingEnumeration.STRING));
        }

        if ("karate".equals(operation)) {
            // Get the expected inputs.
            var configFilePath = ITBUtils.getRequiredString(processRequest.getInput(), "configFilePath");
            LOG.info("Received config file path (from test case) for Karate Runner: [{}].", configFilePath);
            //call access token generator
            LOG.info("Calling Karate Runner");
            Map<String, Object> karateResults = KarateHandler.runKarateTests(configFilePath);

            // Produce the resulting report.
            response.getOutput()
                    .add(ITBUtils.createAnyContentSimple("result", String.valueOf((boolean) karateResults.get("allPassed")), ValueEmbeddingEnumeration.STRING));
            response.getOutput()
                    .add(ITBUtils.createAnyContentSimple("resultDetails", karateResults.toString(), ValueEmbeddingEnumeration.STRING));
        }


        response.setReport(ITBUtils.createReport(TestResultType.SUCCESS));
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
