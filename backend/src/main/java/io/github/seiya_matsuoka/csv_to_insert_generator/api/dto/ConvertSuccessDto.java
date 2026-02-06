package io.github.seiya_matsuoka.csv_to_insert_generator.api.dto;

import java.util.Objects;

/**
 * /convert 成功レスポンスDTO。
 *
 * @param ok 成否（常にtrue）
 * @param generatedAt レスポンス生成時刻（ISO-8601文字列）
 * @param outputFileName ダウンロード想定のファイル名（例: insert_users_20260201_123456.sql）
 * @param sql 生成されたSQL本文
 */
public record ConvertSuccessDto(boolean ok, String generatedAt, String outputFileName, String sql)
    implements ConvertResponseDto {

  /**
   * 成功レスポンスを生成する（ok=true固定）。
   *
   * @param generatedAt レスポンス生成時刻（ISO-8601文字列）
   * @param outputFileName 出力ファイル名
   * @param sql SQL本文
   */
  public ConvertSuccessDto(String generatedAt, String outputFileName, String sql) {
    this(
        true,
        Objects.requireNonNull(generatedAt, "generatedAt is required"),
        Objects.requireNonNull(outputFileName, "outputFileName is required"),
        Objects.requireNonNull(sql, "sql is required"));
  }
}
