package eu.europa.ec.fhir.handlers;

import eu.europa.ec.fhir.pseudo.PseudonymGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * Component to handle the pseudonymization of FHIR resources.
 */
@Component
public class PseudonymizationHandler{

       /** Logger. */
       private static final Logger LOG = LoggerFactory.getLogger(PseudonymizationHandler.class);


       /**
        * Generate a pseudonym for a patient using the provided certificate.
        *
        * @param configFilePath The path to the configuration file.  The configuFilePath can be feed by testbed test case.
        */
       public String pseudoGenerator(String configFilePath, String ssinInput) {
           String expectedPatient = null;
           File configFile = new File(configFilePath);
           PseudonymGenerator generator = new PseudonymGenerator();
           if (!configFilePath.isBlank()) {
               expectedPatient = generator.generateBase64EncodedPseudonym(configFile, ssinInput);
               LOG.info(String.format("Pseudonymised patient info : [%s]:. ", expectedPatient));
           } else {
               LOG.info("configFilePath must be provided.");
           }
           return expectedPatient;
       }

}
