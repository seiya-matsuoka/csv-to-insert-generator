package io.github.seiya_matsuoka.csv_to_insert_generator.tokenize;

import io.github.seiya_matsuoka.csv_to_insert_generator.domain.ColumnType;
import io.github.seiya_matsuoka.csv_to_insert_generator.domain.ValueToken;
import io.github.seiya_matsuoka.csv_to_insert_generator.validation.ErrorCollector;
import io.github.seiya_matsuoka.csv_to_insert_generator.validation.ValidationError;
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
