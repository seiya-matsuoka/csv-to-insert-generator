package io.github.seiya_matsuoka.csv_to_insert_generator.api.dto;

import io.github.seiya_matsuoka.csv_to_insert_generator.validation.ValidationError;
import java.util.Objects;

/**
 * バリデーションエラーDTO。
 *
 * <p>エラー表示に含める項目： 行番号（ファイル先頭から） / 列名 / 型 / 入力値 / 理由
 *
 * @param fileLineNumber ファイル先頭からの行番号（1始まり）
 * @param columnName 列名（ヘッダのカラム名）
 * @param type 型（text/int/... の識別子文字列）
 * @param inputValue 入力値（CSVの生値）
 * @param reason エラー理由
 */
public record ErrorDto(
    int fileLineNumber, String columnName, String type, String inputValue, String reason) {

  /**
   * ドメインのValidationErrorからDTOへ変換する。
   *
   * @param error ValidationError
   * @return ErrorDto
   */
  public static ErrorDto from(ValidationError error) {
    Objects.requireNonNull(error, "error is required");

    return new ErrorDto(
        error.fileLine(), error.columnName(), error.type(), error.input(), error.reason());
  }
}
