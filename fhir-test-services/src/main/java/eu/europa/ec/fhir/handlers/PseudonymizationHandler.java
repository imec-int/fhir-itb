package eu.europa.ec.fhir.handlers;

import com.gitb.ms.Void;
import com.gitb.ms.*;
import com.gitb.tr.TestResultType;
import eu.europa.ec.fhir.handlers.FhirClient;
import eu.europa.ec.fhir.state.ExpectedPost;
import eu.europa.ec.fhir.state.StateManager;
import eu.europa.ec.fhir.utils.Utils;
import jakarta.annotation.Resource;
import jakarta.xml.ws.WebServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import org.imec.ivlab.ehealth.automation.PseudonymGenerator;

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
       public String pseudoGenerator(String configFilePath, String ssin) {
           String certificateFilePath = null; // Placeholder, should be provided
           String expectedPatient = null;
           // Load configuration files
           Properties config = new Properties();
           try (FileInputStream input = new FileInputStream(configFilePath)) {
               config.load(input);
           } catch (IOException e) {
               LOG.info("Config file not found at specified location. Using default values.");
           }
           certificateFilePath = config.getProperty("certificateFilePath", certificateFilePath);
           PseudonymGenerator generator = new PseudonymGenerator();
           if (certificateFilePath != null) {
               System.out.println(certificateFilePath);
               File certificateFile = new File(certificateFilePath);
                expectedPatient = generator.generatePseudonym(certificateFile,ssin);
               LOG.info(String.format("Pseudonymised patient info : [%s]:. ", expectedPatient));
           } else {
               LOG.info("Either base64EncodedString or certificateFilePath must be provided.");
           }
           return expectedPatient;
       }
//
//       public static void main(String[] args) {
//           PseudonymizationHandler instance = new PseudonymizationHandler();
//           instance.pseudoGenerator("resources/config.properties");
//       }
}
