package eu.europa.ec.fhir;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.imec.ivlab.ehealth.automation.PseudonymizationApp;

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
//        new  PseudonymizationApp().run(args)
        SpringApplication.run(Application.class, args);

    }
}


