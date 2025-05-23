package eu.europa.ec.fhir;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point to bootstrap the application.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
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
