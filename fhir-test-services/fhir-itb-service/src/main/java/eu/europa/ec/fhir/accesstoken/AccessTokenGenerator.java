package eu.europa.ec.fhir.accesstoken;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Map;
import java.util.Properties;

public class AccessTokenGenerator {

    private static final String KEYSTORE_TYPE = "PKCS12"; // or "JKS"
    private static final String KEYSTORE_KEY_ALIAS = "authentication";

//    public static void main(String[] args) {
//        AccessTokenGenerator accessTokenGenerator = new AccessTokenGenerator();
//        String accessToken = accessTokenGenerator.generateAccessToken(new File("resources/config.properties"));
//    }

    public String generateAccessToken(File configFilePath) {
        Config config = loadConfig(configFilePath);
        try {
            String jwt = createJWT(config);
            String accessToken = fetchAccessToken(config.accessTokenUrl, jwt);
            System.out.println("jwt = " + jwt);
            System.out.println("accessToken = " + accessToken);
            return accessToken;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate a JWT based on the certificate", e);
        }
    }

    private static String fetchAccessToken(String url, String clientAssertion) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        // Create the request body
        String requestBody = "grant_type=client_credentials"
                + "&client_assertion_type=urn:ietf:params:oauth:client-assertion-type:jwt-bearer"
                + "&client_assertion=" + clientAssertion;

        // Build the request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        // Send the request and get the response
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Parse the access token from the response
        return parseAccessToken(response.body());
    }

    private static String parseAccessToken(String jsonResponse) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        Map responseMap = objectMapper.readValue(jsonResponse, Map.class);
        return (String) responseMap.get("access_token");
    }


    private String createJWT(Config config) throws Exception {
        // Load the keystore
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
        File certificateFile = new File(config.certificateFilePath);
        try (InputStream keyStoreStream = new FileInputStream(certificateFile)) {
            keyStore.load(keyStoreStream, config.certificatePassword.toCharArray());
        }

        PrivateKey privateKey = (PrivateKey) keyStore.getKey(KEYSTORE_KEY_ALIAS, config.certificatePassword.toCharArray());

        return createJWT(privateKey, config.clientId);
    }

    private static String createJWT(PrivateKey privateKey, String subject) {
        return Jwts.builder()
                .subject(subject)
                .issuer(subject)
                .expiration(// 1 hour
                        new java.util.Date(System.currentTimeMillis() + 3600 * 1000))
                .audience().add("https://api-acpt.ehealth.fgov.be/auth/realms/M2M").and()
                .id(//random id
                        java.util.UUID.randomUUID().toString())
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    private static Claims validateJWT(String token, PublicKey publicKey) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .build().parseSignedClaims(token).getPayload();
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

    private record Config(String certificateFilePath, String certificatePassword, String clientId, String accessTokenUrl) {}

    private Config loadConfig(File configFilePath) {
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream(configFilePath)) {
            properties.load(input);
            String certificateFilePath = (getRequiredProperty(properties, "certificateFilePath"));
            String certificatePassword = (getRequiredProperty(properties, "certificatePassword"));
            String clientId = (getRequiredProperty(properties, "client.id"));
            String accessTokenUrl = (getRequiredProperty(properties, "accesstoken.url"));
            return new Config(certificateFilePath, certificatePassword, clientId, accessTokenUrl);
        } catch (IOException e) {
            throw new RuntimeException("Error reading config file: " + e.getMessage(), e);
        }
    }


}
