package eu.europa.ec.fhir.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

public final class HttpUtils {
    // TODO: find out which headers are in fact restricted
    private static final String[] RESTRICTED_HEADERS = {"host", "connection", "content-length", "accept-encoding", "accept-charset", "transfer-encoding", "accept-encoding"};
    private static final String[] RESTRICTED_HEADERS_PREFIXES = {"proxy-", "sec-"};

    public static boolean isRestrictedHeader(String headerName) {
        return Arrays.stream(RESTRICTED_HEADERS)
                .anyMatch(headerName::equalsIgnoreCase)
                || Arrays.stream(RESTRICTED_HEADERS_PREFIXES)
                .anyMatch(headerName::startsWith);
    }

    public static HttpHeaders extractHeaders(HttpServletRequest request) {
        return Collections.list(request.getHeaderNames()).stream()
                .collect(HttpHeaders::new, (httpHeaders, headerName) -> {
                    Collections.list(request.getHeaders(headerName))
                            .forEach(value -> {
                                if (!isRestrictedHeader(headerName)) {
                                    httpHeaders.add(headerName, value);
                                }
                            });
                }, HttpHeaders::putAll);
    }

    public static HttpRequest.BodyPublisher getBodyPublisher(HttpServletRequest request) {
        try {
            var inputStream = request.getInputStream();
            return HttpRequest.BodyPublishers.ofInputStream(() -> inputStream);
        } catch (IOException e) {
            return HttpRequest.BodyPublishers.noBody();
        }
    }

    /**
     * Return the query string of the request, including the leading '?'.
     */
    public static Optional<String> getQueryString(HttpServletRequest request) {
        return Optional.ofNullable(request.getQueryString())
                .map(queryString -> "?" + queryString);

    }
}
