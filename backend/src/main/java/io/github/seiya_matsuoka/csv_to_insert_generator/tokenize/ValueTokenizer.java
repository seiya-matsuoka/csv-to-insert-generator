package io.github.seiya_matsuoka.csv_to_insert_generator.tokenize;

import io.github.seiya_matsuoka.csv_to_insert_generator.csv.CsvRow;
import io.github.seiya_matsuoka.csv_to_insert_generator.csv.ParsedCsv;
import io.github.seiya_matsuoka.csv_to_insert_generator.domain.ColumnType;
import io.github.seiya_matsuoka.csv_to_insert_generator.domain.ValueToken;
import io.github.seiya_matsuoka.csv_to_insert_generator.validation.ErrorCollector;
import io.github.seiya_matsuoka.csv_to_insert_generator.validation.ValidationError;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * ParsedCsv（生文字列）を入力として、セル値を ValueToken（NULL/DEFAULT/空文字/RAW）に解釈する。
 *
 * <p>このクラスはキーワード/空欄の値解釈のみを担当し、型の妥当性チェック（intが数字か等）は行わない。
 */
public final class ValueTokenizer {

  /**
   * 空文字を表すトークン文字列（ダブルクォート2文字）。
   *
   * <p>CSV上では、セルに {@code ""}（2つの " 文字）を入れたものが Commons CSV で {@code "\"\""} として取得できる想定。
   */
  private static final String EMPTY_STRING_TOKEN = "\"\"";

  /** NULLキーワード。 */
  private static final String NULL_KEYWORD = "NULL";

  /** DEFAULTキーワード。 */
  private static final String DEFAULT_KEYWORD = "DEFAULT";

  /**
   * 値をトークン化する。
   *
   * @param parsed CSVパース結果
   * @return トークン化結果（成功/失敗）
   * @throws NullPointerException parsedがnullの場合
   * @throws IllegalArgumentException types数とheaders数が一致しない場合（想定外の入力）
   */
  public ValueTokenizationResult tokenize(ParsedCsv parsed) {
    Objects.requireNonNull(parsed, "parsed is required");

    // CSVパース工程で types数とheader数は一致している想定だが、
    // 想定外の入力が流入した場合、以降の工程でバグの原因になるためチェックする。
    if (parsed.types().size() != parsed.headers().size()) {
      throw new IllegalArgumentException(
          "#typesの列数とヘッダの列数が一致していません: types="
              + parsed.types().size()
              + ", headers="
              + parsed.headers().size());
    }

    // 上限（ErrorCollector）を超えるまでエラーはできるだけ集める。
    ErrorCollector collector = new ErrorCollector();

    List<TokenizedRow> tokenizedRows = new ArrayList<>();
    int columnCount = parsed.headers().size(); // 以後、各行でこの列数を前提にループする

    // 4行目以降のデータ行を順に処理する
    for (CsvRow row : parsed.rows()) {
      // 1データ行分のセル値を順に解釈して ValueToken に変換する
      List<ValueToken> tokens = new ArrayList<>(columnCount);

      for (int i = 0; i < columnCount; i++) {
        // エラー表示のために、列名（header）と型（types）を同じインデックスで参照する
        String columnName = parsed.headers().get(i);
        ColumnType type = parsed.types().get(i);

        // CSVの生値（Commons CSVがクォート解除などした後の文字列）を取得する
        String raw = row.valueAt(i);

        // セル値の内容を解釈してトークン化する（NULL/DEFAULT/空文字/RAW）
        ValueToken token = interpretCell(raw, type, row.fileLine(), columnName, collector);

        // エラーが発生してもできるだけエラーを集めるため、処理は継続する。
        // ただし、上限超過（truncated）になったら、それ以上は追加せず早期終了する。
        tokens.add(token);

        if (collector.isTruncated()) {
          break;
        }
      }

      // 1行分のトークン化した結果を保存する（エラーがあった場合も、エラー収集のためにここまでは作成する）
      tokenizedRows.add(new TokenizedRow(row.fileLine(), tokens));

      if (collector.isTruncated()) {
        break;
      }
    }

    // 失敗時：エラーが1件でもあれば失敗として返す（TokenizedCsvは返さない）
    // 成功時のみ TokenizedCsv を返すことで、後工程は成功前提で組みやすくする。
    if (collector.hasErrors()) {
      return ValueTokenizationResult.failure(collector.errors(), collector.isTruncated());
    }

    // 成功時：ParsedCsvのメタ情報（tableName/types/headers）を引き継ぎ、
    // rowsだけ TokenizedRow に差し替えた TokenizedCsv を返す。
    TokenizedCsv tokenized =
        new TokenizedCsv(parsed.tableName(), parsed.types(), parsed.headers(), tokenizedRows);

    return ValueTokenizationResult.success(tokenized);
  }

  /**
   * 1セル分の値を解釈してトークン化する。
   *
   * <p>解釈ルール:
   *
   * <ul>
   *   <li>空欄（""）: NULL
   *   <li>"NULL": NULL
   *   <li>"DEFAULT": DEFAULT
   *   <li>"\"\""（ダブルクォート2文字）: text型なら空文字、text以外ならエラー
   *   <li>その他: RAW
   * </ul>
   *
   * @param raw 入力セル値（Commons CSVで取得した文字列。nullの場合もあるため許容）
   * @param type 列の型
   * @param fileLine ファイル先頭からの行番号（1始まり）
   * @param columnName 列名（エラー表示用）
   * @param collector エラー収集器
   * @return 生成した ValueToken
   * @throws NullPointerException type/columnName/collector が null の場合
   */
  private ValueToken interpretCell(
      String raw, ColumnType type, int fileLine, String columnName, ErrorCollector collector) {
    Objects.requireNonNull(type, "type is required");
    Objects.requireNonNull(columnName, "columnName is required");
    Objects.requireNonNull(collector, "collector is required");

    // Commons CSVの仕様や入力によっては raw が null の可能性があるため、空欄と同じ扱いに正規化する（＝NULL扱い）
    String original = (raw == null) ? "" : raw;

    // ---- ルール判定は「特別扱い」→「キーワード」→「空文字トークン」→「それ以外」の順 ----

    // 空欄（長さ0）は NULL
    if (original.isEmpty()) {
      return ValueToken.ofNull(original);
    }

    // "NULL" は NULL
    if (original.equals(NULL_KEYWORD)) {
      return ValueToken.ofNull(original);
    }

    // "DEFAULT" は DEFAULT
    if (original.equals(DEFAULT_KEYWORD)) {
      return ValueToken.ofDefault(original);
    }

    // ""（ダブルクォート2文字）は「空文字」を明示するトークン
    if (original.equals(EMPTY_STRING_TOKEN)) {
      // 空文字は text 型のみ許可。他の型で空文字を許すと「NULLとの混同」や「検証・生成の曖昧さ」が出るため、ここで弾く。
      if (type == ColumnType.TEXT) {
        return ValueToken.ofEmptyString(original);
      }

      // text以外で空文字トークンが来たらエラー
      collector.add(
          new ValidationError(
              fileLine, columnName, type.id(), original, "text型以外では空文字トークン(\"\"\")は使用できません"));

      // 失敗時はこの結果は採用されない想定だが、処理継続のためRAWとして返しておく。
      // 呼び出し元の tokens.add(token)  の整合を保ち、以降の列処理も続けられる。
      return ValueToken.ofRaw(original);
    }

    // それ以外は生値（RAW）。 例: "123", "true", "1990-01-02", "hello" など
    return ValueToken.ofRaw(original);
  }
}
