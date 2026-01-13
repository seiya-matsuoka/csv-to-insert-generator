package io.github.seiya_matsuoka.csv_to_insert_generator.csv;

import io.github.seiya_matsuoka.csv_to_insert_generator.validation.ColumnType;
import io.github.seiya_matsuoka.csv_to_insert_generator.validation.ErrorCollector;
import io.github.seiya_matsuoka.csv_to_insert_generator.validation.ValidationError;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
   * CSV文字列をパースする。
   *
   * @param csvText CSV文字列（UTF-8想定。先頭BOMは許容し除去する）
   * @return パース結果（成功/失敗）
   */
  public CsvParseResult parse(String csvText) {
    // エラーは可能な限り収集するが、上限超過時は収集を打ち切る（ErrorCollectorの方針）
    ErrorCollector collector = new ErrorCollector();

    // Windows/ExcelのCSVでは先頭にUTF-8 BOMが付与されることがあるが、BOMが残ると "#table=" 判定が失敗するのでここで除去する。
    String normalized = BomUtil.stripUtf8Bom(csvText);

    // 入力が空/空白のみの場合は、この時点でフォーマット不正として扱う（#table行/#types行/ヘッダ行が存在しないため）
    if (normalized == null || normalized.isBlank()) {
      collector.add(
          new ValidationError(1, "#file", "format", "", "CSVが空です（#table行/#types行/ヘッダ行が必要です）"));
      return CsvParseResult.failure(collector.errors(), collector.isTruncated());
    }

    // Commons CSVで全レコードを一括読み取りする（ここでは行構造の解析までが目的）
    List<CSVRecord> records;
    try {
      records = readAllRecords(normalized);
    } catch (IOException e) {
      // StringReader由来では通常発生しないが、万一に備えてフォーマットエラーとして返す
      collector.add(
          new ValidationError(1, "#file", "format", "", "CSVの読み取りに失敗しました: " + e.getMessage()));
      return CsvParseResult.failure(collector.errors(), collector.isTruncated());
    }

    // フォーマットDは最低でも3行（#table/#types/header）が必要のため検証
    if (records.size() < 3) {
      collector.add(
          new ValidationError(
              1, "#file", "format", "", "CSVの行数が不足しています（#table行/#types行/ヘッダ行が必要です）"));
      return CsvParseResult.failure(collector.errors(), collector.isTruncated());
    }

    // --- ここからフォーマットDの各行を順に処理する ---
    // 1行目: #table=...
    // テーブル名が空/不正なら、この時点でエラー収集（tableNameは空文字で返す）
    String tableName = parseTableLine(records.get(0), collector);
    if (collector.isTruncated()) {
      return CsvParseResult.failure(collector.errors(), true);
    }

    // 2行目: #types=...
    // 型名を ColumnType に解決できない（未知型）場合もエラーとして収集
    List<ColumnType> types = parseTypesLine(records.get(1), collector);
    if (collector.isTruncated()) {
      return CsvParseResult.failure(collector.errors(), true);
    }

    // 3行目: header
    // 列名の形式/重複を検証しつつ取得
    List<String> headers = parseHeaderLine(records.get(2), collector);
    if (collector.isTruncated()) {
      return CsvParseResult.failure(collector.errors(), true);
    }

    // types数とheader数の一致を検証
    // 型検証/SQL生成 時には「列単位で types と headers が1:1対応している」ことが前提となるため、ここで列数不一致を検出してエラーにする。
    if (!types.isEmpty() && !headers.isEmpty() && types.size() != headers.size()) {
      collector.add(
          new ValidationError(
              fileLine(records.get(2)),
              "#header",
              "format",
              "types=" + types.size() + ", headers=" + headers.size(),
              "#typesの列数とヘッダの列数が一致していません"));
    }
    if (collector.isTruncated()) {
      return CsvParseResult.failure(collector.errors(), true);
    }

    // 4行目以降: data rows
    // 行として読み取れるか/ヘッダ列数と一致しているかを見る。
    List<CsvRow> rows = new ArrayList<>();
    int expectedColumns = headers.size();

    for (int i = 3; i < records.size(); i++) {
      CSVRecord r = records.get(i);

      // 列数で厳密に判定する。
      if (r.size() != expectedColumns) {
        collector.add(
            new ValidationError(
                fileLine(r),
                "#data",
                "format",
                joinRecord(r),
                "データ行の列数がヘッダと一致していません（expected=" + expectedColumns + ", actual=" + r.size() + "）"));
        if (collector.isTruncated()) {
          break;
        }
        // 列数が違う行は CsvRow 化できないため、次の行へ
        continue;
      }

      // ここでは生の文字列として保持する。クォート解除などは Commons CSV が行った値（r.get(c)）をそのまま使う。
      List<String> values = new ArrayList<>(expectedColumns);
      for (int c = 0; c < expectedColumns; c++) {
        values.add(r.get(c));
      }
      rows.add(new CsvRow(fileLine(r), values));
    }

    // ここまででエラーが1件でもあれば失敗として返す。
    if (collector.hasErrors()) {
      return CsvParseResult.failure(collector.errors(), collector.isTruncated());
    }

    // 成功時は ParsedCsv として返す
    ParsedCsv parsed = new ParsedCsv(tableName, types, headers, rows);
    return CsvParseResult.success(parsed);
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
