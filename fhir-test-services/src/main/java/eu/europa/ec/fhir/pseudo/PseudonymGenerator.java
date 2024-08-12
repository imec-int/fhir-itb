package eu.europa.ec.fhir.pseudo;

import be.smals.vas.integrations.helper.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Properties;
import java.util.Set;

public class PseudonymGenerator {

    /**
     * Generates a Base64 encoded pseudonym using the configuration specified in the given file.
     *
     * @param configFilePath The file containing the configuration properties.
     * @return A Base64 encoded pseudonym string.
     */
    public String generateBase64EncodedPseudonym(File configFilePath) {
        String domainKey;
        String clientId;
        String realm;
        String certificatePassword;
        String identifier;
        String ehealthFromHeaderValue;
        String ssin;
        String certificateFilePath;

        // Load properties from config file if provided
        if (configFilePath != null && configFilePath.exists()) {
            Properties config = new Properties();

            try (FileInputStream input = new FileInputStream(configFilePath)) {
                config.load(input);
                System.out.println(config.toString());
                // Get values from properties, error if any are missing
                domainKey = getRequiredProperty(config, "domain.key");
                clientId = getRequiredProperty(config, "client.id");
                realm = getRequiredProperty(config, "realm");
                certificatePassword = getRequiredProperty(config, "certificatePassword");
                identifier = getRequiredProperty(config, "identifier");
                ehealthFromHeaderValue = getRequiredProperty(config, "ehealth.from.header.value");
                ssin = getRequiredProperty(config, "ssin");
                certificateFilePath = getRequiredProperty(config, "certificateFilePath");
            } catch (IOException e) {
                throw new RuntimeException("Error reading config file: " + e.getMessage(), e);
            }
        } else {
            throw new IllegalArgumentException("Config file not found at specified location.");
        }

        File certificateFile = new File(certificateFilePath);

        // Set up the private key supplier using the provided certificate and password
        StandardPrivateKeySupplier privateKeySupplier = new StandardPrivateKeySupplier(
                new StandardKeystoreSupplier(certificateFile, certificatePassword),
                new StandardKeystoreEntryPasswordProtectionSupplier(certificatePassword)
        );

        Set<String> foreignDomain = Set.of(domainKey);
        URI jku = URI.create(String.format(
                "https://api-acpt.ehealth.fgov.be/etee/v1/pubKeys/cacerts/jwks?identifier=%s&type=CBE&applicationIdentifier=%s&use=enc",
                identifier, clientId
        ));

        // Create the REST client for pseudonymisation
        StandardPseudonymisationClient restClient = StandardPseudonymisationClient.builder()
                .clientId(clientId)
                .realmUrl(realm)
                .pseudoUrl("https://api-acpt.ehealth.fgov.be/pseudo/v1")
                .privateKeySupplier(privateKeySupplier)
                .fromHeaderValue(ehealthFromHeaderValue)
                .build();

        // Use the pseudonymisation helper to generate a pseudonym
        PseudonymisationHelper pseudonymisationHelper = new PseudonymisationHelper(
                domainKey,
                foreignDomain,
                jku,
                restClient,
                new StandardJwksClient(),
                privateKeySupplier
        );

        // Refresh domains and generate a pseudonym in transit
        pseudonymisationHelper.refreshDomains();
        PseudonymInTransit pseudonymInTransit = pseudonymisationHelper
                .getDomain()
                .valueFactory()
                .from(ssin)
                .pseudonymize();

        // Convert the pseudonym in transit to a DTO
        PseudoInTransitDto pseudoInTransitDto = new PseudoInTransitDto();
        pseudoInTransitDto.setX(pseudonymInTransit.xAsBase64());
        pseudoInTransitDto.setY(pseudonymInTransit.yAsBase64());
        pseudoInTransitDto.setTransitInfo(pseudonymInTransit.transitInfo().asString());
        pseudoInTransitDto.setExp(pseudonymInTransit.exp());
        pseudoInTransitDto.setIat(pseudonymInTransit.iat());
        pseudoInTransitDto.setCrv("P-521"); // Include the crv property, hardcoded to: P-521

        // Serialize the DTO to JSON
        ObjectMapper objectMapper = new ObjectMapper();
        String json;
        try {
            json = objectMapper.writeValueAsString(pseudoInTransitDto);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing pseudonym DTO: " + e.getMessage(), e);
        }

        // Encode the JSON string to Base64
        String base64EncodedPseudonym = Base64.getUrlEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        System.out.println("base64EncodedPseudonym = " + base64EncodedPseudonym);
        return base64EncodedPseudonym;
    }

    /**
     * Retrieves a required property from the configuration. Throws an error if the property is not found.
     *
     * @param config   The Properties object containing configuration values.
     * @param property The name of the property to retrieve.
     * @return The value of the requested property.
     * @throws IllegalArgumentException if the property is missing.
     */
    private String getRequiredProperty(Properties config, String property) {
        String value = config.getProperty(property);
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Missing required configuration property: " + property);
        }
        return value;
    }

    public static void main() {
        PseudonymGenerator generator = new PseudonymGenerator();
        File configFile = new File("resources/config.properties");
        try {
            String pseudonym = generator.generateBase64EncodedPseudonym(configFile);
            System.out.println("Generated Pseudonym: " + pseudonym);
        } catch (Exception e) {
            System.err.println("Error generating pseudonym: " + e.getMessage());
        }
    }
}
