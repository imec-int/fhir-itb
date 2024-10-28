package eu.europa.ec.fhir.http;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;

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
     * Copies the Headers from the given HttpServletRequest into a new HttpHeaders object.
     */
    public static HttpHeaders cloneHeaders(HttpServletRequest fromRequest) {
        HttpHeaders headers = new HttpHeaders();
        copyHeaders(fromRequest, headers);
        return headers;
    }

    /**
     * Copies the headers of the given HttpServletRequest into the given HttpHeaders object.
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

    /**
     * Return the query string of the HttpServletRequest, including the leading '?'.
     */
    public static Optional<String> getQueryString(HttpServletRequest request) {
        return Optional.ofNullable(request.getQueryString())
                .map(queryString -> "?" + queryString);

    }
}
