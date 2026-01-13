package io.github.seiya_matsuoka.csv_to_insert_generator.csv;

import io.github.seiya_matsuoka.csv_to_insert_generator.validation.ErrorCollector;
import io.github.seiya_matsuoka.csv_to_insert_generator.validation.ValidationError;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

/**
 * CSVフォーマットDを読み取り、メタ行/ヘッダ/列数の整合性を検証しつつパースする。
 *
 * <p>フォーマットDの仕様:
 *
 * <ol>
 *   <li>1行目: {@code #table=<table>}
 *   <li>2行目: {@code #types=<type1>,<type2>,...}
 *   <li>3行目: {@code <col1>,<col2>,...}
 *   <li>4行目以降: {@code <val1>,<val2>,...}
 * </ol>
 *
 * <p>このクラスは 値解釈（NULL/DEFAULT/空文字） や 型検証 までは行わず、 主に以下をチェックする:
 *
 * <ul>
 *   <li>メタ行の存在と形式（#table / #types）
 *   <li>未知の型（ColumnTypeに存在しない型）
 *   <li>types数とheader数の一致
 *   <li>header名の形式と重複
 *   <li>データ行の列数一致
 * </ul>
 */
public final class CsvFormatDParser {

  private static final String TABLE_PREFIX = "#table=";
  private static final String TYPES_PREFIX = "#types=";

  // SQL生成時に安全な識別子として扱いやすくするため、ここで制限する（schema未対応、識別子は英数_、先頭は英字or_）
  private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*$");

  private final CSVFormat csvFormat;

  /** デフォルトのCSVFormatで生成する。 */
  public CsvFormatDParser() {
    // 空行も行として扱いたいので ignoreEmptyLines=false にしておく
    this.csvFormat = CSVFormat.DEFAULT.builder().setIgnoreEmptyLines(false).build();
  }

  /**
   * CSVの全レコードを読み取る。
   *
   * @param csvText CSV文字列
   * @return レコード一覧
   * @throws IOException 読み取りに失敗した場合
   */
  private List<CSVRecord> readAllRecords(String csvText) throws IOException {
    try (CSVParser parser = new CSVParser(new StringReader(csvText), csvFormat)) {
      return parser.getRecords();
    }
  }

  /**
   * 1行目（#table=...）を解析し、テーブル名を返す。
   *
   * <p>不正な場合は ValidationError を追加し、空文字を返す。
   *
   * @param record 1行目のCSVRecord
   * @param collector エラー収集器
   * @return テーブル名（不正な場合は空文字）
   */
  private String parseTableLine(CSVRecord record, ErrorCollector collector) {
    int line = fileLine(record);

    // #table=... の形式は1列のみを前提とする
    if (record.size() != 1) {
      collector.add(
          new ValidationError(
              line, "#table", "format", joinRecord(record), "#table行は1列のみである必要があります"));
      return "";
    }

    // 先頭セルに "#table=" が付いているかを確認する
    String cell = record.get(0);
    if (!cell.startsWith(TABLE_PREFIX)) {
      collector.add(
          new ValidationError(
              line, "#table", "format", cell, "#table行は '#table=<tableName>' 形式である必要があります"));
      return "";
    }

    // "#table=" 以降がテーブル名
    String tableName = cell.substring(TABLE_PREFIX.length());
    if (tableName.isEmpty()) {
      collector.add(new ValidationError(line, "#table", "format", cell, "テーブル名が空です"));
      return "";
    }

    // SQL生成を安全に行うため、識別子として妥当かをチェックする
    if (!IDENTIFIER_PATTERN.matcher(tableName).matches()) {
      collector.add(
          new ValidationError(
              line, "#table", "format", tableName, "テーブル名が不正です（英数字と_のみ、先頭は英字または_）"));
      return "";
    }

    return tableName;
  }

  /**
   * 2行目（#types=...）を解析し、列型の一覧を返す。
   *
   * <p>未知型や空の型はエラーとして収集する。
   *
   * @param record 2行目のCSVRecord
   * @param collector エラー収集器
   * @return 型一覧（エラーがあっても可能な範囲で解決した結果）
   */
  private List<ColumnType> parseTypesLine(CSVRecord record, ErrorCollector collector) {
    int line = fileLine(record);

    if (record.size() < 1) {
      collector.add(new ValidationError(line, "#types", "format", "", "#types行が不正です"));
      return List.of();
    }

    // 先頭セルは "#types=int" のように prefix + 1個目の型の構成である必要がある
    String first = record.get(0);
    if (!first.startsWith(TYPES_PREFIX)) {
      collector.add(
          new ValidationError(
              line,
              "#types",
              "format",
              first,
              "#types行は '#types=<type1>,<type2>,...' 形式である必要があります"));
      return List.of();
    }

    // 1セル目が "#types=int" のようになるため、1セル目から "#types=" を除去して1個目の型とし、2セル目以降を残りの型として扱う。
    String firstTypeRaw = first.substring(TYPES_PREFIX.length());

    List<String> typeStrings = new ArrayList<>();
    typeStrings.add(firstTypeRaw);
    for (int i = 1; i < record.size(); i++) {
      typeStrings.add(record.get(i));
    }

    // 文字列型名 から ColumnType へ解決する（未知型はエラー）
    List<ColumnType> types = new ArrayList<>();
    for (String raw : typeStrings) {
      String s = raw == null ? "" : raw.trim();

      // 型指定が空なのはフォーマット不正（列の対応関係が作れないため）
      if (s.isEmpty()) {
        collector.add(
            new ValidationError(line, "#types", "format", raw == null ? "" : raw, "型が空です"));
        if (collector.isTruncated()) {
          break;
        }
        continue;
      }

      // fromId は Optional を返すため、未知型を明示的にエラーとして扱える
      ColumnType.fromId(s)
          .ifPresentOrElse(
              types::add,
              () ->
                  collector.add(
                      new ValidationError(
                          line,
                          "#types",
                          "format",
                          s,
                          "未知の型です（許可している型: text/int/decimal/bool/date/timestamp/uuid）")));

      if (collector.isTruncated()) {
        break;
      }
    }

    return types;
  }

  /**
   * 3行目（ヘッダ行）を解析し、列名一覧を返す。
   *
   * <p>列名の形式チェック・重複チェックを行い、問題があればエラーとして収集する。
   *
   * @param record 3行目のCSVRecord
   * @param collector エラー収集器
   * @return 列名一覧
   */
  private List<String> parseHeaderLine(CSVRecord record, ErrorCollector collector) {
    int line = fileLine(record);

    if (record.size() < 1) {
      collector.add(new ValidationError(line, "#header", "format", "", "ヘッダ行が空です"));
      return List.of();
    }

    List<String> headers = new ArrayList<>(record.size());
    Set<String> seen = new HashSet<>();

    for (int i = 0; i < record.size(); i++) {
      String col = record.get(i);

      // 空列名は列の対応関係が崩れるため、エラー
      if (col == null || col.isEmpty()) {
        collector.add(
            new ValidationError(line, "#header", "format", "", "列名が空です（列" + (i + 1) + "）"));
        if (collector.isTruncated()) {
          break;
        }
        headers.add("");
        continue;
      }

      // SQL生成を安全に行うため、識別子として妥当かもチェックする
      if (!IDENTIFIER_PATTERN.matcher(col).matches()) {
        collector.add(new ValidationError(line, col, "format", col, "列名が不正です（英数字と_のみ、先頭は英字または_）"));
        if (collector.isTruncated()) {
          break;
        }
      }

      // 重複列名はSQL生成時の列対応が曖昧になるためエラーとする
      if (!seen.add(col)) {
        collector.add(new ValidationError(line, col, "format", col, "列名が重複しています"));
        if (collector.isTruncated()) {
          break;
        }
      }

      headers.add(col);
    }

    return headers;
  }

  /**
   * CSVRecord から「ファイル先頭からの行番号（1始まり）」を取得する。
   *
   * @param record CSVRecord
   * @return 行番号（1始まり）
   * @throws ArithmeticException longがintに収まらない場合
   */
  private static int fileLine(CSVRecord record) {
    // CSVRecord#getRecordNumber は1始まり（long）
    return Math.toIntExact(record.getRecordNumber());
  }

  /**
   * レコード内容をエラー表示用に簡易的に結合する。
   *
   * <p>デバッグ/表示用途として見せるための情報。
   *
   * @param record CSVRecord
   * @return カンマ結合した文字列
   */
  private static String joinRecord(CSVRecord record) {
    // エラー表示用に、CSVRecordの内容を簡易的に結合する
    List<String> parts = new ArrayList<>();
    for (int i = 0; i < record.size(); i++) {
      parts.add(record.get(i));
    }
    return String.join(",", parts);
  }
}
