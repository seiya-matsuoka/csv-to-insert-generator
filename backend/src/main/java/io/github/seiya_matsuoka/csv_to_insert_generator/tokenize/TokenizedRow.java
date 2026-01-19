package io.github.seiya_matsuoka.csv_to_insert_generator.tokenize;

import io.github.seiya_matsuoka.csv_to_insert_generator.domain.ValueToken;
import java.util.List;
import java.util.Objects;

/**
 * トークン化されたデータ行（4行目以降）を表すモデル。
 *
 * @param fileLine ファイル先頭からの行番号（1始まり）
 * @param tokens セルごとの値トークン
 * @throws IllegalArgumentException fileLineが1未満の場合
 * @throws NullPointerException tokens が null の場合
 */
public record TokenizedRow(int fileLine, List<ValueToken> tokens) {

  public TokenizedRow {
    if (fileLine <= 0) {
      throw new IllegalArgumentException("fileLine は1以上でなければいけません: " + fileLine);
    }
    tokens = List.copyOf(Objects.requireNonNull(tokens, "tokens is required"));
  }

  /**
   * 指定列のトークンを返す。
   *
   * @param index 0始まりの列インデックス
   * @return 値トークン
   * @throws IndexOutOfBoundsException indexが範囲外の場合
   */
  public ValueToken tokenAt(int index) {
    return tokens.get(index);
  }
}
