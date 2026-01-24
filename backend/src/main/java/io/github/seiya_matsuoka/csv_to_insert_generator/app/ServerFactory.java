package io.github.seiya_matsuoka.csv_to_insert_generator.app;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.concurrent.Executors;

/** HttpServer生成ユーティリティ。 */
public final class ServerFactory {

  private ServerFactory() {}

  /**
   * HttpServerを生成する。
   *
   * @param port リッスンポート
   * @param rootHandler ルートハンドラ（通常 Router）
   * @return HttpServer
   * @throws IOException I/O例外
   */
  public static HttpServer create(int port, HttpHandler rootHandler) throws IOException {
    Objects.requireNonNull(rootHandler, "rootHandler is required");

    HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
    server.createContext("/", rootHandler);

    // シンプルにキャッシュスレッドプール
    server.setExecutor(Executors.newCachedThreadPool());
    return server;
  }
}
