package eu.europa.ec.fhir.gitb;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.cxf.Bus;
import org.apache.cxf.jaxws.EndpointImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.xml.namespace.QName;

/**
 * Configuration class responsible for creating the Spring beans required by the service.
 */
@Configuration
public class BeanConfig {

    /**
     * The FHIR proxy messaging service endpoint.
     *
     * @return The endpoint.
     */
    @Bean
    public EndpointImpl messagingService(Bus cxfBus, ProxyMessagingService serviceImplementation) {
        EndpointImpl endpoint = new EndpointImpl(cxfBus, serviceImplementation);
        endpoint.setServiceName(new QName("http://www.gitb.com/ms/v1/", "MessagingServiceService"));
        endpoint.setEndpointName(new QName("http://www.gitb.com/ms/v1/", "MessagingServicePort"));
        endpoint.publish("/messaging/proxy");
        return endpoint;
    }

    /**
     * The FHIR resource validation service endpoint.
     *
     * @return The endpoint.
     */
    @Bean
    public EndpointImpl validationService(Bus cxfBus, FhirResourceValidationService serviceImplementation) {
        EndpointImpl endpoint = new EndpointImpl(cxfBus, serviceImplementation);
        endpoint.setServiceName(new QName("http://www.gitb.com/vs/v1/", "ValidationService"));
        endpoint.setEndpointName(new QName("http://www.gitb.com/vs/v1/", "ValidationServicePort"));
        endpoint.publish("/validation/fhir");
        return endpoint;
    }

    /**
     * The processing service endpoint.
     *
     * @return The endpoint.
     */
    @Bean
    public EndpointImpl processingService(Bus cxfBus, ProcessingServiceImpl serviceImplementation) {
        EndpointImpl endpoint = new EndpointImpl(cxfBus, serviceImplementation);
        endpoint.setServiceName(new QName("http://www.gitb.com/ps/v1/", "ProcessingServiceService"));
        endpoint.setEndpointName(new QName("http://www.gitb.com/ps/v1/", "ProcessingServicePort"));
        endpoint.publish("/process");
        return endpoint;
    }

    /**
     * Jackson object mapper to process JSON content.
     *
     * @return The mapper.
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

}
