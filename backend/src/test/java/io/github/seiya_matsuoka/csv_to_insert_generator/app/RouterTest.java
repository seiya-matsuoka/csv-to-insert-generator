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

/**
 * {@link Router} のテスト。
 *
 * <p>ここでは HttpServer を実際に起動せず、Router#handle に対して HttpExchange のスタブ（FakeHttpExchange）を渡して振る舞いを確認する。
 */
public class RouterTest {

  // OPTIONS（プリフライト）はルート登録なしでも 204 を返すことを確認
  // Origin が許可されている場合、CORSヘッダ（Allow-Origin）が付与されることを確認
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

  // ルート登録がないパスに対して 404 を返すことを確認
  @Test
  void shouldReturnNotFound_whenNoRouteMatches() throws Exception {

    CorsPolicy cors = CorsPolicy.fromCommaSeparated("http://localhost:5173", List.of());
    Router router = new Router(cors);

    FakeHttpExchange ex = new FakeHttpExchange("GET", "http://localhost:8080/unknown");

    router.handle(ex);

    assertEquals(404, ex.statusCode());
    assertTrue(new String(ex.responseBodyBytes(), StandardCharsets.UTF_8).contains("Not Found"));
  }

  // method + path が一致する場合、登録したハンドラが呼ばれることを確認
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

  // /healthz が 200 を返し、ボディが "ok" であることを確認（JSON化しない方針）
  @Test
  void shouldReturnOk_whenHealthzIsRequested() throws Exception {

    AppFactory factory = new AppFactory();
    CorsPolicy cors = factory.buildCorsPolicy("http://localhost:5173");
    Router router = factory.buildRouter(cors);

    FakeHttpExchange ex = new FakeHttpExchange("GET", "http://localhost:8080/healthz");

    router.handle(ex);

    assertEquals(200, ex.statusCode());
    assertEquals("ok", new String(ex.responseBodyBytes(), StandardCharsets.UTF_8));
  }

  /**
   * Routerテスト用の簡易 HttpExchange スタブ。
   *
   * <p>Router が参照するのは主に:
   *
   * <ul>
   *   <li>getRequestHeaders（Origin 等）
   *   <li>getResponseHeaders（CORSヘッダ付与を確認）
   *   <li>getRequestMethod
   *   <li>getRequestURI（path判定）
   *   <li>sendResponseHeaders / getResponseBody（結果確認）
   * </ul>
   *
   * <p>それ以外は今回のテストでは使わないため、null/ダミーで実装する。
   */
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
      // 実際の HttpServer はここでレスポンスの開始を確定するが、テストではステータスコードが分かれば十分なので保持するだけにする
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
      // RouterテストではContextを使わないため null
      return null;
    }

    @Override
    public HttpPrincipal getPrincipal() {
      // 認証は扱っていないため null
      return null;
    }
  }
}
