package io.github.seiya_matsuoka.csv_to_insert_generator.tokenize;

import io.github.seiya_matsuoka.csv_to_insert_generator.domain.ColumnType;
import java.util.List;
import java.util.Objects;

/**
 * 解析済みCSV（ParsedCsv）を「値トークン（NULL/DEFAULT/空文字/RAW）」へ変換した結果を表すモデル。
 *
 * @param tableName テーブル名
 * @param types 列の型
 * @param headers 列名
 * @param rows トークン化されたデータ行（4行目以降）
 * @throws NullPointerException いずれかがnullの場合
 */
public record TokenizedCsv(
    String tableName, List<ColumnType> types, List<String> headers, List<TokenizedRow> rows) {
  public TokenizedCsv {
    tableName = Objects.requireNonNull(tableName, "tableName is required");
    types = List.copyOf(Objects.requireNonNull(types, "types is required"));
    headers = List.copyOf(Objects.requireNonNull(headers, "headers is required"));
    rows = List.copyOf(Objects.requireNonNull(rows, "rows is required"));
  }
}
