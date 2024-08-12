package be.smals.vas.integrations.helper;

import static be.smals.vas.integrations.helper.utils.SneakyThrowUtil.sneakyThrow;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;

public class StandardJwksClient implements JwksClient {

  private final HttpClient httpClient;
  private final Duration readTimeout;

  /**
   * Constructor with infinite timeouts and no proxy.
   */
  public StandardJwksClient() {
    this(null, null);
  }

  /**
   * @param connectTimeout The HTTP connects timeout, in milliseconds, zero for infinite. Must not be negative. 0 means infinite timeout.
   * @param readTimeout    The HTTP read timeout, in milliseconds, zero for infinite. Must not be negative. 0 means infinite timeout.
   */
  public StandardJwksClient(final Duration connectTimeout, final Duration readTimeout) {
    this.readTimeout = readTimeout;
    final HttpClient.Builder builder = HttpClient.newBuilder();
    if (connectTimeout != null) {
      builder.connectTimeout(connectTimeout);
    }
    this.httpClient = builder.build();
  }

  @Override
  public String getJwks(final URI jwksUrl) {
    try {
      final var request = HttpRequest.newBuilder().uri(jwksUrl).GET();
      if (readTimeout != null) {
        request.timeout(readTimeout);
      }
      final var response = httpClient.send(request.build(), BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        throw new RuntimeException("GET " + jwksUrl + " returned code " + response.statusCode() + ": " + response.body());
      }
      return response.body();
    } catch (final IOException | InterruptedException e) {
      return sneakyThrow(e);
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private Duration readTimeout;
    private Duration connectionTimeout;

    public Builder readTimeout(final Duration readTimeout) {
      this.readTimeout = readTimeout;
      return this;
    }

    public Builder connectionTimeout(final Duration connectionTimeout) {
      this.connectionTimeout = connectionTimeout;
      return this;
    }

    public StandardJwksClient build() {
      return new StandardJwksClient(connectionTimeout, readTimeout);
    }
  }
}
