package io.github.seiya_matsuoka.csv_to_insert_generator.validation;

import java.util.Objects;

/**
 * 検証エラー1件を表すモデル。
 *
 * <p>行番号は「ファイル先頭からの行番号（1始まり）」を採用する。 例: 1行目が #table=...、2行目が #types=...、3行目がヘッダ、4行目以降がデータ。
 *
 * @param fileLine ファイル先頭からの行番号（1始まり、1以上）
 * @param columnName エラー対象の列名（例: {@code "id"}）
 * @param type 列の型（例: {@code "int"}）。未知型などの場合は入力文字列のままでもよい
 * @param input 入力値（CSVセルの生値 / 表示用）
 * @param reason エラー理由（期待形式や不正内容など）
 * @throws IllegalArgumentException fileLineが1未満の場合
 * @throws NullPointerException columnName/type/input/reason が null の場合
 */
public record ValidationError(
    int fileLine, String columnName, String type, String input, String reason) {

  /**
   * コンストラクタ（recordのcanonical constructor）で最低限のバリデーションを行う。
   *
   * @throws IllegalArgumentException fileLineが1未満の場合
   * @throws NullPointerException 必須項目がnullの場合
   */
  public ValidationError {
    if (fileLine <= 0) {
      throw new IllegalArgumentException("fileLine は1以上でなければいけません: " + fileLine);
    }
    columnName = Objects.requireNonNull(columnName, "columnName is required");
    type = Objects.requireNonNull(type, "type is required");
    input = Objects.requireNonNull(input, "input is required");
    reason = Objects.requireNonNull(reason, "reason is required");
  }
}
