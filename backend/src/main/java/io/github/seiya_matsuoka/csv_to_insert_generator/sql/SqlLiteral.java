package io.github.seiya_matsuoka.csv_to_insert_generator.sql;

import io.github.seiya_matsuoka.csv_to_insert_generator.domain.ColumnType;
import io.github.seiya_matsuoka.csv_to_insert_generator.domain.ValueToken;
import java.util.Objects;

/**
 * ValueToken と ColumnType からSQLリテラル（または NULL/DEFAULT）文字列を生成する。
 *
 * <ul>
 *   <li>ValueToken は tokenize + validator を通過済みの想定
 *   <li>ただし防御的に、想定外の状態でも安全側に倒す（例: RAW null）
 *   <li>返す文字列は、INSERT文のVALUES節にそのまま埋め込める形式。
 * </ul>
 */
public final class SqlLiteral {

  /**
   * ValueToken を SQL表現に変換する。
   *
   * @param type 列型
   * @param token 値トークン
   * @return SQL表現（例: NULL, DEFAULT, 'text', 123, TRUE）
   * @throws NullPointerException type または token が null の場合
   */
  public String toSql(ColumnType type, ValueToken token) {
    Objects.requireNonNull(type, "type is required");
    Objects.requireNonNull(token, "token is required");

    return switch (token.kind()) {
      case NULL -> "NULL";
      case DEFAULT -> "DEFAULT";
      case EMPTY_STRING -> "''";
      case RAW -> rawToSql(type, token.value().orElse(""));
    };
  }

  /**
   * RAW値を型に応じてSQL表現へ変換する。
   *
   * <ul>
   *   <li>ここでは妥当性の検証は行わない（前工程で実施済み想定）
   *   <li>SQLとしての表現（クォート/エスケープ/TRUE/FALSEなど）にのみ責務を持つ
   * </ul>
   *
   * @param type 列型
   * @param raw RAW文字列（nullは来ない想定だが防御的に空扱い）
   * @return SQL表現
   */
  private String rawToSql(ColumnType type, String raw) {
    String s = raw == null ? "" : raw;

    return switch (type) {
      case TEXT -> quoteText(s);

      // 数値はクォートしない（検証済み想定のため）
      case INT, DECIMAL -> s.trim();

      // boolはTRUE/FALSEに統一（大小文字は normalize）
      case BOOL -> normalizeBool(s);

      // 日付/時刻/UUIDはシングルクォートで囲む
      case DATE, TIMESTAMP, UUID -> quoteText(s.trim());
    };
  }

  /**
   * 文字列をSQL文字列リテラルとしてクォートし、シングルクォートをエスケープする。
   *
   * @param s 入力文字列
   * @return 例: 'O''Reilly'
   */
  private String quoteText(String s) {
    // SQL標準のシングルクォートは '' に置換してエスケープ
    String escaped = (s == null ? "" : s).replace("'", "''");
    return "'" + escaped + "'";
  }

  /**
   * bool表現をTRUE/FALSEへ正規化する。
   *
   * @param s 入力文字列
   * @return TRUE または FALSE
   */
  private String normalizeBool(String s) {
    String lower = (s == null ? "" : s.trim()).toLowerCase();
    // validatorの工程で true/false のみ許可されている想定
    return lower.equals("true") ? "TRUE" : "FALSE";
  }
}
