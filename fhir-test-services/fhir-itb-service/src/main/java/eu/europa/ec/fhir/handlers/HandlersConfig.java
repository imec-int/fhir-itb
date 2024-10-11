package eu.europa.ec.fhir.handlers;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@Configuration
public class HandlersConfig {
    @Bean
    HttpClient httpClient() {
        return HttpClient.newHttpClient();
    }

    @Bean
    ItbRestClient itbRestClient() {
        // TODO: load values from config
        var baseUrl = "http://localhost:9000/api/rest";

        var restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("ITB_API_KEY", "FC2C742FX2585X4FE8X9982X200350CDC523")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .build();

        return new ItbRestClient(restClient);
    }

    @Bean
    RestClient restClient() {
        return RestClient.builder().build();
    }

}
