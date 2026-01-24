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
 * <ul>
 *   <li>CORSヘッダ付与（許可Originのみ）
 *   <li>OPTIONS（プリフライト）への応答（204）
 *   <li>ハンドラ例外の一括500化
 * </ul>
 */
public final class Router implements HttpHandler {

  private final CorsPolicy corsPolicy;
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
    // すべてのレスポンスに対して、Originが許可されていればCORSヘッダを付ける
    corsPolicy.apply(exchange.getRequestHeaders(), exchange.getResponseHeaders());

    String method = normalizeMethod(exchange.getRequestMethod());

    // プリフライト（OPTIONS）は全パス共通で204を返す（ルーティング不要）
    if (corsPolicy.isOptions(method)) {
      HttpResponses.sendNoContent(exchange, 204);
      return;
    }

    String path = normalizePath(exchange.getRequestURI());
    RouteHandler handler = routes.get(routeKey(method, path));

    if (handler == null) {
      HttpResponses.sendText(exchange, 404, "Not Found");
      return;
    }

    try {
      handler.handle(exchange);
    } catch (Exception e) {
      // 想定外例外はここでまとめて500に落とす
      HttpResponses.sendText(exchange, 500, "Internal Server Error");
    }
  }

  /** method + path をキーにする。 */
  private String routeKey(String method, String path) {
    return normalizeMethod(method) + " " + normalizePath(URI.create(path));
  }

  /** HTTPメソッドを正規化する。 */
  private String normalizeMethod(String method) {
    return (method == null ? "" : method.trim().toUpperCase(Locale.ROOT));
  }

  /** URIからパスを正規化する（末尾スラッシュ揺れを吸収）。 */
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
