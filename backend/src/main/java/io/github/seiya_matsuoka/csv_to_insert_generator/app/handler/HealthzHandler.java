package io.github.seiya_matsuoka.csv_to_insert_generator.app.handler;

import com.sun.net.httpserver.HttpExchange;
import io.github.seiya_matsuoka.csv_to_insert_generator.app.HttpResponses;
import io.github.seiya_matsuoka.csv_to_insert_generator.app.RouteHandler;

/**
 * ヘルスチェック用ハンドラ。
 *
 * <p>監視や疎通確認で利用する。固定で "ok" を返す。
 */
public final class HealthzHandler implements RouteHandler {

  /**
   * リクエストを処理する。
   *
   * @param exchange HTTP交換オブジェクト
   * @throws Exception 処理に失敗した場合
   */
  @Override
  public void handle(HttpExchange exchange) throws Exception {
    // レスポンスは "ok" のみ（JSON化しない）
    HttpResponses.sendText(exchange, 200, "ok");
  }
}
