package io.github.seiya_matsuoka.csv_to_insert_generator.csv;

import io.github.seiya_matsuoka.csv_to_insert_generator.domain.ColumnType;
import java.util.List;
import java.util.Objects;

/**
 * CSVフォーマットDのパース結果（成功時）を表すモデル。
 *
 * @param tableName テーブル名（#table=... から取得）
 * @param types 列の型（#types=... から取得）
 * @param headers 列名（ヘッダ行から取得）
 * @param rows データ行（4行目以降）
 * @throws NullPointerException いずれかがnullの場合
 */
public record ParsedCsv(
    String tableName, List<ColumnType> types, List<String> headers, List<CsvRow> rows) {
  public ParsedCsv {
    tableName = Objects.requireNonNull(tableName, "tableName is required");
    types = List.copyOf(Objects.requireNonNull(types, "types is required"));
    headers = List.copyOf(Objects.requireNonNull(headers, "headers is required"));
    rows = List.copyOf(Objects.requireNonNull(rows, "rows is required"));
  }
}
