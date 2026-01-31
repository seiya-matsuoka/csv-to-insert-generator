package io.github.seiya_matsuoka.csv_to_insert_generator.app;

import com.sun.net.httpserver.HttpServer;

/**
 * バックエンドAPI起動エントリポイント。
 *
 * <p>Renderは PORT 環境変数を渡すため、それを優先して使用する。
 *
 * <p>ローカル開発では PORT 未設定が多いので、デフォルト（8080）にフォールバックする。
 *
 * <p>分離デプロイ（Vercel → Render）を想定しているため、 CORS 設定（ALLOWED_ORIGINS）も起動時に読み取って Router に適用する。
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
    // PORT 決定（Render: PORT が必ず渡される想定、Local: 8080 にフォールバック）
    int port = readPortOrDefault(System.getenv("PORT"), 8080);

    // 依存（CORS/Router/UseCase 等）を組み立てる。FW無しのため、new の集約は Factory に寄せて見通しを良くする
    AppFactory factory = new AppFactory();
    CorsPolicy corsPolicy = factory.buildCorsPolicy(System.getenv("ALLOWED_ORIGINS"));
    Router router = factory.buildRouter(corsPolicy);

    // HttpServer を生成して起動。
    HttpServer server = ServerFactory.create(port, router);
    server.start();

    System.out.println("CSV to INSERT Generator started on port " + port);
  }

  /**
   * PORT環境変数を読み取り、整数に変換する。
   *
   * <p>値が不正（整数でない）な場合も、起動を止めずに defaultPort へフォールバックする（原因が追えるよう起動自体は継続する方針）
   *
   * @param portEnv 環境変数 PORT の値
   * @param defaultPort デフォルトポート
   * @return 使用するポート番号
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
