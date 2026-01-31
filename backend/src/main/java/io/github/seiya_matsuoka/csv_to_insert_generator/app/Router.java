package io.github.seiya_matsuoka.csv_to_insert_generator.app;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * method + path でハンドラを切り替える簡易ルータ。
 *
 * <p>FW無しのため、最低限のルーティングを自前で用意する。
 *
 * <p>ルータは「ルーティング」と「HTTP共通処理」を担当し、ビジネスロジックは handler/usecase に寄せる。
 *
 * <p>共通機能:
 *
 * <ul>
 *   <li>CORSヘッダ付与（許可Originのみ）
 *   <li>OPTIONS（プリフライト）への共通応答（204）
 *   <li>ハンドラ例外の一括500化
 * </ul>
 */
public final class Router implements HttpHandler {

  private final CorsPolicy corsPolicy;

  /**
   * ルートテーブル。
   *
   * <p>キーは "METHOD /path" の形式（例: "GET /healthz"）。 method は大文字、path は末尾スラッシュ揺れを吸収した正規形で扱う。
   */
  private final Map<String, RouteHandler> routes = new HashMap<>();

  /**
   * コンストラクタ。
   *
   * @param corsPolicy CORSポリシー
   */
  public Router(CorsPolicy corsPolicy) {
    this.corsPolicy = Objects.requireNonNull(corsPolicy, "corsPolicy is required");
  }

  /**
   * ルートを登録する。
   *
   * <p>例: register("GET", "/healthz", handler)
   *
   * @param method HTTPメソッド（GET/POSTなど）
   * @param path パス（例: /healthz）
   * @param handler ハンドラ
   */
  public void register(String method, String path, RouteHandler handler) {
    Objects.requireNonNull(method, "method is required");
    Objects.requireNonNull(path, "path is required");
    Objects.requireNonNull(handler, "handler is required");

    String key = routeKey(method, path);
    routes.put(key, handler);
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    // CORSヘッダの付与。Origin が許可されている場合のみ、レスポンスに必要なヘッダを付ける
    corsPolicy.apply(exchange.getRequestHeaders(), exchange.getResponseHeaders());

    // メソッド/パスの正規化
    String method = normalizeMethod(exchange.getRequestMethod());

    // ブラウザのCORS仕様上、POST/JSON等ではプリフライトが飛ぶ可能性があるため、
    // ルート登録が無くても プリフライト（OPTIONS）は全パス共通で204 を返せるようにする。
    if (corsPolicy.isOptions(method)) {
      HttpResponses.sendNoContent(exchange, 204);
      return;
    }

    String path = normalizePath(exchange.getRequestURI());

    // ルーティング（METHOD + PATH）
    RouteHandler handler = routes.get(routeKey(method, path));

    if (handler == null) {
      // まだ登録していないパスは 404 を返す
      HttpResponses.sendText(exchange, 404, "Not Found");
      return;
    }

    // ハンドラ実行（例外はここで一括処理）
    try {
      handler.handle(exchange);
    } catch (Exception e) {
      // 想定外例外はここでまとめてシンプルに 500 として返す。
      HttpResponses.sendText(exchange, 500, "Internal Server Error");
    }
  }

  /**
   * method + path をキーにする。
   *
   * @param method HTTPメソッド
   * @param path パス
   * @return ルートキー
   */
  private String routeKey(String method, String path) {
    return normalizeMethod(method) + " " + normalizePath(URI.create(path));
  }

  /** HTTPメソッドを正規化する（null/空対策 + 大文字化）。 */
  private String normalizeMethod(String method) {
    return (method == null ? "" : method.trim().toUpperCase(Locale.ROOT));
  }

  /**
   * URIからパスを正規化する（末尾スラッシュ揺れを吸収）。
   *
   * <p>例:
   *
   * <ul>
   *   <li>"/healthz/" -> "/healthz"
   *   <li>"/" -> "/"（そのまま）
   * </ul>
   */
  private String normalizePath(URI uri) {
    String p = uri == null ? "/" : uri.getPath();
    if (p == null || p.isBlank()) {
      return "/";
    }
    String path = p.trim();
    // "/healthz/" のような末尾スラッシュは "/healthz" に寄せる（ただし "/" はそのまま）
    if (path.length() > 1 && path.endsWith("/")) {
      return path.substring(0, path.length() - 1);
    }
    return path;
  }
}
