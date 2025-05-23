package eu.europa.ec.fhir.handlers;

import com.intuit.karate.Results;
import com.intuit.karate.Runner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class KarateHandler {
    private static final Logger LOG = LoggerFactory.getLogger(KarateHandler.class);

    public static Map<String, Object> runKarateTests(String configFilePath) {
        // Run the Karate tests and store results
        Results results = Runner.path(configFilePath)
                .parallel(5); // Run tests in parallel with 5 threads

        // Create a map to store the results
        Map<String, Object> testResults = new HashMap<>();

        // Check if all tests passed
        boolean allPassed = results.getFailCount() == 0;
        testResults.put("allPassed", allPassed);

        // Add the pass/fail status for each feature
        results.getScenarioResults().forEach(result -> {
            // Add each feature name and its pass status
            String featureName = result.getScenario()
                    .getFeature()
                    .getResource()
                    .getRelativePath();
            testResults.put(featureName, result.isFailed() ? "failed" : "passed");
        });

        LOG.info(String.format("Karate RUNNER Test results: [%s].", results));
        return testResults;

    }

}
