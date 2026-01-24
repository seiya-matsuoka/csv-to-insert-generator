package io.github.seiya_matsuoka.csv_to_insert_generator.app;

import io.github.seiya_matsuoka.csv_to_insert_generator.usecase.ConvertUseCase;
import java.util.List;

/**
 * アプリの依存（Router/UseCaseなど）を組み立てるFactory。
 *
 * <p>フレームワーク無しのため、手動DI（手でnewして渡す）をここに集約する。
 */
public final class AppFactory {

  /**
   * CORSポリシーを構築する。
   *
   * @param allowedOriginsEnv ALLOWED_ORIGINS（カンマ区切り）
   * @return CorsPolicy
   */
  public CorsPolicy buildCorsPolicy(String allowedOriginsEnv) {
    // ローカル開発のデフォルト（env未設定のときに最低限ブラウザ動作できる）
    List<String> defaults = List.of("http://localhost:5173");
    return CorsPolicy.fromCommaSeparated(allowedOriginsEnv, defaults);
  }

  /**
   * Router を構築する。
   *
   * <p>/healthz や /convert を register する。
   *
   * @param corsPolicy CORSポリシー
   * @return Router
   */
  public Router buildRouter(CorsPolicy corsPolicy) {
    return new Router(corsPolicy);
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
