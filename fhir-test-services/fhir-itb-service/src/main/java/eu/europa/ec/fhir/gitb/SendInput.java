package eu.europa.ec.fhir.gitb;

import com.gitb.ms.SendRequest;
import eu.europa.ec.fhir.utils.ITBUtils;
import org.springframework.http.HttpMethod;

import java.util.Arrays;

public record SendInput(
        HttpMethod method,
        String endpoint,
        String payload,
        String token
) {

    public static SendInput fromRequest(SendRequest request) throws IllegalArgumentException {
        var input = request.getInput();
        var type = ITBUtils.getRequiredString(input, "type");
        var method = HttpMethod.valueOf(type.toUpperCase());
        if (Arrays.stream(HttpMethod.values())
                .noneMatch(m -> m.equals(method))) {
            throw new IllegalArgumentException("Unsupported type [%s] for 'send' operation.".formatted(method.toString()));
        }

        var endpoint = ITBUtils.getRequiredString(input, "endpoint");
        var payload = ITBUtils.getRequiredString(input, "payload");
        var authorizationToken = ITBUtils.getRequiredString(input, "authorizationToken");

        return new SendInput(method, endpoint, payload, authorizationToken);
    }
}
