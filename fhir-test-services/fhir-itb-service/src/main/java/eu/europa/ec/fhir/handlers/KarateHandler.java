package eu.europa.ec.fhir.handlers;

import org.springframework.stereotype.Component;

import com.intuit.karate.Results;
import com.intuit.karate.Runner;
import com.intuit.karate.cli.IntellijHook;

@Component
public class KarateHandler {

    public static void runKarateTests() {
        // Create a builder for Karate Runner with custom listener
        var builder = Runner.builder()
                .path("resources/karate/karate-demo.feature") // Corrected path to use classpath
                .hook(new IntellijHook());
        builder.timeoutMinutes(1); // Increased timeout to 5 minutes
        builder.parallel(1);
    }
}
