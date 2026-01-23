package io.github.seiya_matsuoka.csv_to_insert_generator.app;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** HttpServer向けのレスポンス送信ユーティリティ。 */
public final class HttpResponses {

  private HttpResponses() {}

  /**
   * プレーンテキストを返す。
   *
   * @param exchange exchange
   * @param status HTTPステータス
   * @param body 本文
   * @throws IOException I/O例外
   */
  public static void sendText(HttpExchange exchange, int status, String body) throws IOException {
    Objects.requireNonNull(exchange, "exchange is required");

    String text = body == null ? "" : body;
    byte[] bytes = text.getBytes(StandardCharsets.UTF_8);

    Headers headers = exchange.getResponseHeaders();
    headers.set("Content-Type", "text/plain; charset=utf-8");
    headers.set("X-Content-Type-Options", "nosniff");

    exchange.sendResponseHeaders(status, bytes.length);
    try (OutputStream os = exchange.getResponseBody()) {
      os.write(bytes);
    }
  }

  /**
   * ボディ無し（No Content）を返す。
   *
   * @param exchange exchange
   * @param status HTTPステータス（通常204）
   * @throws IOException I/O例外
   */
  public static void sendNoContent(HttpExchange exchange, int status) throws IOException {
    Objects.requireNonNull(exchange, "exchange is required");

    exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
    exchange.sendResponseHeaders(status, -1);
    exchange.close();
  }
}
