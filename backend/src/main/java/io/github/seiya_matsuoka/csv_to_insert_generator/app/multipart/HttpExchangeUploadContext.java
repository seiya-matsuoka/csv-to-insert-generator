package io.github.seiya_matsuoka.csv_to_insert_generator.app.multipart;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.apache.commons.fileupload.UploadContext;

/**
 * {@link HttpExchange} を Apache Commons FileUpload の {@link UploadContext} に適合させるアダプタ。
 *
 * <p>FileUploadは本来Servlet向けだが、低レベルAPI（RequestContext/UploadContext）を使うことで
 * HttpServer（HttpExchange）でもmultipart解析ができる。
 */
public final class HttpExchangeUploadContext implements UploadContext {

  private final HttpExchange exchange;
  private final String contentType;
  private final long contentLength;

  /**
   * アダプタを生成する。
   *
   * @param exchange HttpExchange
   */
  public HttpExchangeUploadContext(HttpExchange exchange) {
    this.exchange = Objects.requireNonNull(exchange, "exchange is required");

    Headers headers = exchange.getRequestHeaders();
    this.contentType = headers.getFirst("Content-Type");
    this.contentLength = parseContentLength(headers.getFirst("Content-Length"));
  }

  /** {@inheritDoc} */
  @Override
  public String getCharacterEncoding() {
    // multipartの各パート文字列はUTF-8前提
    return StandardCharsets.UTF_8.name();
  }

  /** {@inheritDoc} */
  @Override
  public String getContentType() {
    return contentType;
  }

  /**
   * {@inheritDoc}
   *
   * <p>commons-fileupload側の互換用メソッド（int）。
   */
  @Override
  public int getContentLength() {
    if (contentLength < 0) {
      return -1;
    }
    if (contentLength > Integer.MAX_VALUE) {
      return Integer.MAX_VALUE;
    }
    return (int) contentLength;
  }

  /** {@inheritDoc} */
  @Override
  public long contentLength() {
    return contentLength;
  }

  /** {@inheritDoc} */
  @Override
  public InputStream getInputStream() throws IOException {
    // HttpExchangeのRequestBodyは一度しか読めない点に注意（FileUploadが消費する）
    return exchange.getRequestBody();
  }

  private static long parseContentLength(String headerValue) {
    if (headerValue == null || headerValue.isBlank()) {
      return -1;
    }
    try {
      return Long.parseLong(headerValue.trim());
    } catch (NumberFormatException e) {
      return -1;
    }
  }
}
