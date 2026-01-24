package io.github.seiya_matsuoka.csv_to_insert_generator.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

/** {@link Router} のテスト。 */
public class RouterTest {

  @Test
  void shouldReturnNoContent_whenOptionsRequestIsGiven() throws Exception {
    CorsPolicy cors = CorsPolicy.fromCommaSeparated("http://localhost:5173", List.of());
    Router router = new Router(cors);

    FakeHttpExchange ex = new FakeHttpExchange("OPTIONS", "http://localhost:8080/convert");
    ex.getRequestHeaders().set("Origin", "http://localhost:5173");

    router.handle(ex);

    assertEquals(204, ex.statusCode());
    assertEquals(
        "http://localhost:5173", ex.getResponseHeaders().getFirst("Access-Control-Allow-Origin"));
  }

  @Test
  void shouldReturnNotFound_whenNoRouteMatches() throws Exception {
    CorsPolicy cors = CorsPolicy.fromCommaSeparated("http://localhost:5173", List.of());
    Router router = new Router(cors);

    FakeHttpExchange ex = new FakeHttpExchange("GET", "http://localhost:8080/unknown");

    router.handle(ex);

    assertEquals(404, ex.statusCode());
    assertTrue(new String(ex.responseBodyBytes(), StandardCharsets.UTF_8).contains("Not Found"));
  }

  @Test
  void shouldInvokeHandler_whenRouteMatches() throws Exception {
    CorsPolicy cors = CorsPolicy.fromCommaSeparated("http://localhost:5173", List.of());
    Router router = new Router(cors);

    router.register("GET", "/ping", e -> HttpResponses.sendText(e, 200, "pong"));

    FakeHttpExchange ex = new FakeHttpExchange("GET", "http://localhost:8080/ping");

    router.handle(ex);

    assertEquals(200, ex.statusCode());
    assertEquals("pong", new String(ex.responseBodyBytes(), StandardCharsets.UTF_8));
  }

  /** Routerテスト用の簡易HttpExchangeスタブ。 */
  private static final class FakeHttpExchange extends HttpExchange {

    private final Headers requestHeaders = new Headers();
    private final Headers responseHeaders = new Headers();
    private final ByteArrayOutputStream responseBody = new ByteArrayOutputStream();

    private final String method;
    private final URI uri;

    private int statusCode = -1;

    FakeHttpExchange(String method, String url) {
      this.method = method;
      this.uri = URI.create(url);
    }

    int statusCode() {
      return statusCode;
    }

    byte[] responseBodyBytes() {
      return responseBody.toByteArray();
    }

    @Override
    public Headers getRequestHeaders() {
      return requestHeaders;
    }

    @Override
    public Headers getResponseHeaders() {
      return responseHeaders;
    }

    @Override
    public URI getRequestURI() {
      return uri;
    }

    @Override
    public String getRequestMethod() {
      return method;
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
      return new InetSocketAddress(0);
    }

    @Override
    public int getResponseCode() {
      return statusCode;
    }

    @Override
    public InetSocketAddress getLocalAddress() {
      return new InetSocketAddress(0);
    }

    @Override
    public void sendResponseHeaders(int rCode, long responseLength) {
      this.statusCode = rCode;
    }

    @Override
    public OutputStream getResponseBody() {
      return responseBody;
    }

    @Override
    public InputStream getRequestBody() {
      return new ByteArrayInputStream(new byte[0]);
    }

    @Override
    public void close() {
      // no-op
    }

    @Override
    public String getProtocol() {
      return "HTTP/1.1";
    }

    @Override
    public Object getAttribute(String name) {
      return null;
    }

    @Override
    public void setAttribute(String name, Object value) {
      // no-op
    }

    @Override
    public void setStreams(InputStream i, OutputStream o) {
      // no-op
    }

    @Override
    public HttpContext getHttpContext() {
      return null;
    }

    @Override
    public HttpPrincipal getPrincipal() {
      return null;
    }
  }
}
