package io.github.seiya_matsuoka.csv_to_insert_generator.app.handler;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import io.github.seiya_matsuoka.csv_to_insert_generator.app.HttpResponses;
import io.github.seiya_matsuoka.csv_to_insert_generator.app.RouteHandler;
import java.io.InputStream;
import java.util.Objects;

/**
 * classpath resources 上の CSV をダウンロードさせるハンドラ。
 *
 * <p>Maven は src/main/resources をビルド時に classpath（target/classes）へコピーするため、 Render
 * 環境でも同じバイナリで配布可能な「固定CSV」を扱える。
 */
public final class CsvDownloadHandler implements RouteHandler {

  private final String resourcePath;
  private final String downloadFilename;

  /**
   * コンストラクタ。
   *
   * @param resourcePath classpath 上のパス（例: "templates/template.csv"）
   * @param downloadFilename ダウンロード時のファイル名（例: "template.csv"）
   */
  public CsvDownloadHandler(String resourcePath, String downloadFilename) {
    this.resourcePath = Objects.requireNonNull(resourcePath, "resourcePath is required");
    this.downloadFilename =
        Objects.requireNonNull(downloadFilename, "downloadFilename is required");
  }

  @Override
  public void handle(HttpExchange exchange) throws Exception {
    // classpath resource を読む（Thread context class loader を使うと、テスト実行時/実行jar時でも扱いやすい）
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    try (InputStream in = cl.getResourceAsStream(resourcePath)) {
      if (in == null) {
        // リソースが見つからない場合は 404
        HttpResponses.sendText(exchange, 404, "Not Found");
        return;
      }

      byte[] bytes = in.readAllBytes();

      // レスポンスヘッダ設定（CSV + ダウンロード想定）
      Headers headers = exchange.getResponseHeaders();
      headers.set("Content-Type", "text/csv; charset=utf-8");

      // ブラウザで「保存」を促すため attachment を付与（filename はシンプルに固定）
      headers.set("Content-Disposition", "attachment; filename=\"" + downloadFilename + "\"");
      headers.set("X-Content-Type-Options", "nosniff");

      // ボディ送信
      exchange.sendResponseHeaders(200, bytes.length);
      exchange.getResponseBody().write(bytes);
      exchange.getResponseBody().close();
    }
  }
}
