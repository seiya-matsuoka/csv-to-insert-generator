package io.github.seiya_matsuoka.csv_to_insert_generator.csv;

import java.util.List;
import java.util.Objects;

/**
 * CSVのデータ行（4行目以降）を表すモデル。
 *
 * @param fileLine ファイル先頭からの行番号（1始まり）
 * @param values その行のセル値（生値、クォート解除後の文字列）
 * @throws IllegalArgumentException fileLineが1未満の場合
 * @throws NullPointerException values が null の場合
 */
public record CsvRow(int fileLine, List<String> values) {

  public CsvRow {
    if (fileLine <= 0) {
      throw new IllegalArgumentException("fileLine は1以上でなければいけません: " + fileLine);
    }
    values = List.copyOf(Objects.requireNonNull(values, "values is required"));
  }

  /**
   * 指定列のセル値を返す。
   *
   * @param index 0始まりの列インデックス
   * @return セル値
   * @throws IndexOutOfBoundsException indexが範囲外の場合
   */
  public String valueAt(int index) {
    return values.get(index);
  }
}
