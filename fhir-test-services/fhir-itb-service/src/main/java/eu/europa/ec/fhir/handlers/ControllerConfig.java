package eu.europa.ec.fhir.handlers;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;

@Configuration
public class ControllerConfig {
    @Bean
    HttpClient httpClient() {
        return HttpClient.newHttpClient();
    }
}
