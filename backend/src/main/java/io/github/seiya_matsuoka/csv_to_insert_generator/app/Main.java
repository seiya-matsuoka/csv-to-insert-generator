package io.github.seiya_matsuoka.csv_to_insert_generator.app;

import com.sun.net.httpserver.HttpServer;

/**
 * バックエンドAPI起動エントリポイント。
 *
 * <p>Renderは PORT 環境変数を渡すため、それを優先して使用する。
 */
public final class Main {

  private Main() {}

  /**
   * アプリ起動。
   *
   * @param args 起動引数（未使用）
   * @throws Exception 起動に失敗した場合
   */
  public static void main(String[] args) throws Exception {
    int port = readPortOrDefault(System.getenv("PORT"), 8080);

    AppFactory factory = new AppFactory();
    CorsPolicy corsPolicy = factory.buildCorsPolicy(System.getenv("ALLOWED_ORIGINS"));
    Router router = factory.buildRouter(corsPolicy);

    HttpServer server = ServerFactory.create(port, router);
    server.start();

    System.out.println("CSV to INSERT Generator started on port " + port);
  }

  /**
   * PORT環境変数を読み取り、整数に変換する。
   *
   * @param portEnv 環境変数PORT
   * @param defaultPort デフォルト
   * @return ポート番号
   */
  private static int readPortOrDefault(String portEnv, int defaultPort) {
    if (portEnv == null || portEnv.isBlank()) {
      return defaultPort;
    }
    try {
      return Integer.parseInt(portEnv.trim());
    } catch (NumberFormatException e) {
      return defaultPort;
    }
  }
}
