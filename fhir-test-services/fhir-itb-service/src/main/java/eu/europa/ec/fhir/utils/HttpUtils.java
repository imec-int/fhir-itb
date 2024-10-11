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

    /**
     * Returns a new HttpHeaders object with the same headers from the given deferredRequest.
     */
    public static HttpHeaders cloneHeaders(HttpServletRequest fromRequest) {
        HttpHeaders headers = new HttpHeaders();
        copyHeaders(fromRequest, headers);
        return headers;
    }

    /**
     * Copies the headers of the given deferredRequest into the given HttpHeaders object.
     * This method does not remove any existing headers in the given HttpHeaders object.
     * Existing headers will be overwritten.
     */
    public static void copyHeaders(HttpServletRequest fromRequest, HttpHeaders intoHeaders) {
        Collections.list(fromRequest.getHeaderNames())
                .forEach((headerName) -> {
                    Collections.list(fromRequest.getHeaders(headerName))
                            .forEach(value -> {
                                if (!isRestrictedHeader(headerName)) {
                                    intoHeaders.add(headerName, value);
                                }
                            });
                });
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
     * Return the query string of the deferredRequest, including the leading '?'.
     */
    public static Optional<String> getQueryString(HttpServletRequest request) {
        return Optional.ofNullable(request.getQueryString())
                .map(queryString -> "?" + queryString);

    }
}
