package io.github.seiya_matsuoka.csv_to_insert_generator.api.dto;

import java.util.List;
import java.util.Objects;

/**
 * /convert 失敗レスポンスDTO。
 *
 * @param ok 成否（常にfalse）
 * @param generatedAt レスポンス生成時刻（ISO-8601文字列）
 * @param errors エラー一覧（最大maxErrors件）
 * @param truncated エラーが上限に達して打ち切られた場合true
 * @param maxErrors 収集上限（例: 100）
 */
public record ConvertFailureDto(
    boolean ok, String generatedAt, List<ErrorDto> errors, boolean truncated, int maxErrors)
    implements ConvertResponseDto {

  /**
   * 失敗レスポンスを生成する（ok=false固定）。
   *
   * @param generatedAt レスポンス生成時刻（ISO-8601文字列）
   * @param errors エラー一覧
   * @param truncated 打ち切りフラグ
   * @param maxErrors 収集上限
   */
  public ConvertFailureDto(
      String generatedAt, List<ErrorDto> errors, boolean truncated, int maxErrors) {
    this(
        false,
        Objects.requireNonNull(generatedAt, "generatedAt is required"),
        List.copyOf(Objects.requireNonNull(errors, "errors is required")),
        truncated,
        maxErrors);
  }
}
