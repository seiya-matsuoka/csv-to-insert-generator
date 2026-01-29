package io.github.seiya_matsuoka.csv_to_insert_generator.app;

import io.github.seiya_matsuoka.csv_to_insert_generator.app.handler.HealthzHandler;
import io.github.seiya_matsuoka.csv_to_insert_generator.usecase.ConvertUseCase;
import java.util.List;

/**
 * アプリの依存（Router/UseCaseなど）を組み立てるFactory。
 *
 * <p>フレームワーク無しのため、手動DI（手でnewして渡す）をここに集約する。
 *
 * <p>new が散らばると追跡が難しくなるため、「どこで何を組み立てるか」をこのクラスに集約する。
 */
public final class AppFactory {

  /**
   * CORSポリシーを構築する。
   *
   * <p>ALLOWED_ORIGINS が未設定の場合でも、ローカル開発で最低限動かせるように localhost をデフォルト許可に含める。
   *
   * @param allowedOriginsEnv ALLOWED_ORIGINS（カンマ区切り）
   * @return CorsPolicy
   */
  public CorsPolicy buildCorsPolicy(String allowedOriginsEnv) {
    // ローカル開発のデフォルト（env未設定のときに最低限ブラウザ動作できる（Vite dev server））
    List<String> defaults = List.of("http://localhost:5173");
    return CorsPolicy.fromCommaSeparated(allowedOriginsEnv, defaults);
  }

  /**
   * Router を構築する。
   *
   * <p>Router 自体は「共通処理（CORS/OPTIONS/例外500化）＋ method+path ルーティング」を担当。
   *
   * <p>/healthz や /convert を register する。
   *
   * @param corsPolicy CORSポリシー
   * @return Router
   */
  public Router buildRouter(CorsPolicy corsPolicy) {
    Router router = new Router(corsPolicy);

    // /healthz を登録。監視/疎通確認用。curlやブラウザから叩けるよう GET で提供する。
    router.register("GET", "/healthz", new HealthzHandler());

    return router;
  }

  /**
   * ConvertUseCase を構築する。
   *
   * <p>POST /convertで使用する。
   *
   * @return ConvertUseCase
   */
  public ConvertUseCase buildConvertUseCase() {
    return new ConvertUseCase();
  }
}
