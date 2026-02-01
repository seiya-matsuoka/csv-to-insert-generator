package io.github.seiya_matsuoka.csv_to_insert_generator.api.dto;

/**
 * /convert のレスポンス共通インターフェース。
 *
 * <p>Jacksonでのシリアライズは、実際に返す具象DTO（success/failure）を ObjectMapperに渡す想定のため、ポリモーフィック設定は不要。
 */
public sealed interface ConvertResponseDto permits ConvertSuccessDto, ConvertFailureDto {

  /**
   * 成否フラグ。
   *
   * @return 成功時true、失敗時false
   */
  boolean ok();

  /**
   * レスポンス生成時刻（ISO-8601文字列）。
   *
   * @return 生成時刻
   */
  String generatedAt();
}
