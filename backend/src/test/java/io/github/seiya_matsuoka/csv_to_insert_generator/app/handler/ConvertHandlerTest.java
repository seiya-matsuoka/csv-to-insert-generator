package io.github.seiya_matsuoka.csv_to_insert_generator.app.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;
import io.github.seiya_matsuoka.csv_to_insert_generator.api.json.ObjectMapperFactory;
import io.github.seiya_matsuoka.csv_to_insert_generator.app.multipart.MultipartCsvExtractor;
import io.github.seiya_matsuoka.csv_to_insert_generator.usecase.ConvertUseCase;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * {@link ConvertHandler} のテスト。
 *
 * <p>multipart/form-data の最小リクエストを自前で組み立て、 ConvertHandler が 200/422/400 のJSONを返すことを確認する。
 */
public class ConvertHandlerTest {

  @Test
  void shouldReturn400_whenRequestIsNotMultipart() throws Exception {

    ConvertHandler handler =
        new ConvertHandler(
            new ConvertUseCase(), new MultipartCsvExtractor(), ObjectMapperFactory.create());

    FakeHttpExchange ex =
        new FakeHttpExchange("POST", "http://localhost:8080/convert", new byte[0]);
    // Content-Type を付けない（= multipart 判定で弾かれる）
    handler.handle(ex);

    assertEquals(400, ex.statusCode());

    String contentType = ex.getResponseHeaders().getFirst("Content-Type");
    assertTrue(contentType != null && contentType.startsWith("application/json"));

    String body = new String(ex.responseBodyBytes(), StandardCharsets.UTF_8);
    assertTrue(body.contains("\"ok\":false"));
  }

  @Test
  void shouldReturn422_whenCsvIsInvalid() throws Exception {

    ConvertHandler handler =
        new ConvertHandler(
            new ConvertUseCase(), new MultipartCsvExtractor(), ObjectMapperFactory.create());

    String boundary = "----boundary-test-422";
    String invalidCsv =
        ""
            + "#table=users\n"
            // #types を欠落させる（仕様上必須）→ 422 を期待
            + "id,name\n"
            + "1,Alice\n";

    byte[] multipart = buildMultipart(boundary, "users", "invalid.csv", invalidCsv);

    FakeHttpExchange ex = new FakeHttpExchange("POST", "http://localhost:8080/convert", multipart);
    ex.getRequestHeaders().set("Content-Type", "multipart/form-data; boundary=" + boundary);

    handler.handle(ex);

    assertEquals(422, ex.statusCode());

    String contentType = ex.getResponseHeaders().getFirst("Content-Type");
    assertTrue(contentType != null && contentType.startsWith("application/json"));

    String body = new String(ex.responseBodyBytes(), StandardCharsets.UTF_8);
    assertTrue(body.contains("\"ok\":false"));
    assertTrue(body.contains("\"errors\""));
  }

  @Test
  void shouldReturn200_whenCsvIsValid() throws Exception {

    ConvertHandler handler =
        new ConvertHandler(
            new ConvertUseCase(), new MultipartCsvExtractor(), ObjectMapperFactory.create());

    String boundary = "----boundary-test-200";
    String validCsv = "" + "#table=users\n" + "#types=int,text\n" + "id,name\n" + "1,Alice\n";

    byte[] multipart = buildMultipart(boundary, "users", "valid.csv", validCsv);

    FakeHttpExchange ex = new FakeHttpExchange("POST", "http://localhost:8080/convert", multipart);
    ex.getRequestHeaders().set("Content-Type", "multipart/form-data; boundary=" + boundary);

    handler.handle(ex);

    assertEquals(200, ex.statusCode());

    String contentType = ex.getResponseHeaders().getFirst("Content-Type");
    assertTrue(contentType != null && contentType.startsWith("application/json"));

    String body = new String(ex.responseBodyBytes(), StandardCharsets.UTF_8);
    assertTrue(body.contains("\"ok\":true"));
    assertTrue(body.contains("\"outputFileName\""));
    assertTrue(body.contains("\"sql\""));
    assertTrue(body.contains("INSERT"));
  }

  /**
   * multipart/form-data の最小ボディを組み立てる。
   *
   * <p>フォーム: - table: テーブル名 - file : CSVファイル（text/csv）
   *
   * @param boundary boundary文字列
   * @param tableName tableフィールドの値
   * @param fileName fileパートのfilename
   * @param csvText CSV本文（UTF-8）
   * @return multipartボディ（UTF-8）
   */
  private static byte[] buildMultipart(
      String boundary, String tableName, String fileName, String csvText) {

    String crlf = "\r\n";

    StringBuilder sb = new StringBuilder();

    // table field
    sb.append("--").append(boundary).append(crlf);
    sb.append("Content-Disposition: form-data; name=\"table\"").append(crlf);
    sb.append(crlf);
    sb.append(tableName).append(crlf);

    // file field
    sb.append("--").append(boundary).append(crlf);
    sb.append("Content-Disposition: form-data; name=\"file\"; filename=\"")
        .append(fileName)
        .append("\"")
        .append(crlf);
    sb.append("Content-Type: text/csv").append(crlf);
    sb.append(crlf);
    sb.append(csvText).append(crlf);

    // end
    sb.append("--").append(boundary).append("--").append(crlf);

    return sb.toString().getBytes(StandardCharsets.UTF_8);
  }

  /**
   * ConvertHandlerテスト用の HttpExchange スタブ。
   *
   * <p>request body（multipart）を流し込み、sendResponseHeaders と response body を保持する。
   */
  private static final class FakeHttpExchange extends HttpExchange {

    private final Headers requestHeaders = new Headers();
    private final Headers responseHeaders = new Headers();
    private final ByteArrayOutputStream responseBody = new ByteArrayOutputStream();

    private final String method;
    private final URI uri;
    private final byte[] requestBodyBytes;

    private int statusCode = -1;

    FakeHttpExchange(String method, String url, byte[] requestBodyBytes) {
      this.method = method;
      this.uri = URI.create(url);
      this.requestBodyBytes = requestBodyBytes == null ? new byte[0] : requestBodyBytes;
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
    public InputStream getRequestBody() {
      return new ByteArrayInputStream(requestBodyBytes);
    }

    @Override
    public OutputStream getResponseBody() {
      return responseBody;
    }

    @Override
    public void sendResponseHeaders(int rCode, long responseLength) {
      this.statusCode = rCode;
    }

    @Override
    public void close() {
      // no-op
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
      return new InetSocketAddress(0);
    }

    @Override
    public InetSocketAddress getLocalAddress() {
      return new InetSocketAddress(0);
    }

    @Override
    public int getResponseCode() {
      return statusCode;
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
