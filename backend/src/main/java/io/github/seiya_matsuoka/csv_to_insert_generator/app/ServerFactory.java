package io.github.seiya_matsuoka.csv_to_insert_generator.app;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.concurrent.Executors;

/**
 * HttpServer 生成ユーティリティ。
 *
 * <p>責務は「ポートと rootHandler を受けて HttpServer を構築する」だけに限定する。
 *
 * <p>ルーティングやビジネスロジックは Router/handler 側で扱う。
 */
public final class ServerFactory {

  private ServerFactory() {}

  /**
   * HttpServer を生成する。
   *
   * @param port リッスンポート（Renderでは PORT env を使用）
   * @param rootHandler ルートハンドラ（通常 Router）
   * @return HttpServer
   * @throws IOException I/O例外
   */
  public static HttpServer create(int port, HttpHandler rootHandler) throws IOException {
    Objects.requireNonNull(rootHandler, "rootHandler is required");

    // InetSocketAddress(port) は 0.0.0.0:<port> 相当でバインドされるため、
    // Render 環境でも外部アクセス可能な形で待ち受けられる。
    HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

    // "/" で受け、Router 側が method+path によって分岐する
    server.createContext("/", rootHandler);

    // 単純なスレッドプールで十分（cachedThreadPool は負荷に応じてスレッドが増減する）
    server.setExecutor(Executors.newCachedThreadPool());

    return server;
  }
}
