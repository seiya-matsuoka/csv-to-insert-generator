package io.github.seiya_matsuoka.csv_to_insert_generator.app;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * HttpServer向けのレスポンス送信ユーティリティ。
 *
 * <p>HttpExchange は「ヘッダ設定→sendResponseHeaders→getResponseBodyへ書き込み」の順序が必須で、
 * ルールを崩すと例外や不正レスポンスになりやすい。
 *
 * <p>よく使うパターンを共通化し、ハンドラ側の見通しを良くする。
 */
public final class HttpResponses {

  private HttpResponses() {}

  /**
   * プレーンテキストを返す。
   *
   * <p>Content-Type を必ず付与し、ブラウザの推測（MIME sniffing）を抑止する。
   *
   * @param exchange exchange
   * @param status HTTPステータス
   * @param body 本文（nullは空扱い）
   * @throws IOException I/O例外
   */
  public static void sendText(HttpExchange exchange, int status, String body) throws IOException {
    Objects.requireNonNull(exchange, "exchange is required");

    String text = body == null ? "" : body;
    byte[] bytes = text.getBytes(StandardCharsets.UTF_8);

    Headers headers = exchange.getResponseHeaders();
    headers.set("Content-Type", "text/plain; charset=utf-8");
    headers.set("X-Content-Type-Options", "nosniff");

    // ここでレスポンスのステータスと長さを確定させる
    exchange.sendResponseHeaders(status, bytes.length);

    // その後に body を書き込む（try-with-resources で確実に close）
    try (OutputStream os = exchange.getResponseBody()) {
      os.write(bytes);
    }
  }

  /**
   * ボディ無し（No Content）を返す。
   *
   * <p>OPTIONS のプリフライト応答などボディ不要のケースで利用する。
   *
   * <p>sendResponseHeaders の length に -1 を指定すると「レスポンスボディ無し」を表せる。
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
