package io.github.seiya_matsuoka.csv_to_insert_generator.app;

import com.sun.net.httpserver.HttpExchange;

/**
 * Router から呼び出されるアプリ側ハンドラ。
 *
 * <p>標準の HttpHandler は checked exception を投げられないため、 ハンドラ内で例外処理が煩雑になりやすい。
 *
 * <p>このインターフェースは throws Exception を許可し、 Router 側で「例外を共通的に 500 に落とす」などの処理を一括管理しやすくする。
 */
@FunctionalInterface
public interface RouteHandler {

  /**
   * リクエストを処理する。
   *
   * @param exchange HTTP交換オブジェクト
   * @throws Exception ハンドラ内で発生した例外（Routerが500に整形する）
   */
  void handle(HttpExchange exchange) throws Exception;
}
