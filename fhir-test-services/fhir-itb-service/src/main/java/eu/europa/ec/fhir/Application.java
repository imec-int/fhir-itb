package eu.europa.ec.fhir;

import eu.europa.ec.fhir.handlers.KarateHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import eu.europa.ec.fhir.handlers.KarateHandler;

/**
 * Entry point to bootstrap the application.
 */
@SpringBootApplication
public class Application {

    /**
     * The application's main method.
     *
     * @param args Runtime arguments (none expected).
     */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}


