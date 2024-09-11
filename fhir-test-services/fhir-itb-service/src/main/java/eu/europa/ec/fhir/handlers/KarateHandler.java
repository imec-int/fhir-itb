package eu.europa.ec.fhir.handlers;

import com.intuit.karate.Results;
import com.intuit.karate.Runner;
import java.util.HashMap;
import java.util.Map;

public class KarateHandler {

    public static Map<String, Object> runKarateTests() {
        // Run the Karate tests and store results
        Results results = Runner.path("resources/karate/scratch.feature") // Corrected path to use classpath // Corrected path using classpath
                .parallel(5); // Run tests in parallel with 5 threads

        // Create a map to store the results
        Map<String, Object> testResults = new HashMap<>();

        // Check if all tests passed
        boolean allPassed = results.getFailCount() == 0;
        testResults.put("allPassed", allPassed);

        // Add the pass/fail status for each feature
        results.getScenarioResults().forEach(result -> {
            // Add each feature name and its pass status
            testResults.put(result.getScenario().getFeature().getResource().toString(), result.isFailed() ? "failed" : "passed");
        });

        return testResults;
    }

//    public static void main(String[] args) {
//        Map<String, Object> results = runKarateTests();
//
//        // Output the results
//        System.out.println("Test results: " + results);
//    }
}
