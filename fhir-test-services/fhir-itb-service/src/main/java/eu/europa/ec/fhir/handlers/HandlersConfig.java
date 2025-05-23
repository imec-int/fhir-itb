package eu.europa.ec.fhir.handlers;

import eu.europa.ec.fhir.proxy.ItbRestClient;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${itb.vendor.api_key}")
    private String ORG_API_KEY;

    @Value("${itb.base_url}")
    private String BASE_URL;

    @Bean
    ItbRestClient itbRestClient() {
        var restClient = RestClient.builder()
                .baseUrl(BASE_URL + "/api/rest")
                .defaultHeader("ITB_API_KEY", ORG_API_KEY)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .build();

        return new ItbRestClient(restClient);
    }

    @Bean
    RestClient restClient() {
        return RestClient.builder().build();
    }

}
