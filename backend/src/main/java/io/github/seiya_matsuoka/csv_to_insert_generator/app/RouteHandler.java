package io.github.seiya_matsuoka.csv_to_insert_generator.app;

import com.sun.net.httpserver.HttpExchange;

/** Router から呼び出されるアプリ側ハンドラ。 */
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
