package io.github.seiya_matsuoka.csv_to_insert_generator.validator;

import io.github.seiya_matsuoka.csv_to_insert_generator.domain.ColumnType;
import io.github.seiya_matsuoka.csv_to_insert_generator.domain.ValueToken;
import io.github.seiya_matsuoka.csv_to_insert_generator.tokenize.TokenizedCsv;
import io.github.seiya_matsuoka.csv_to_insert_generator.tokenize.TokenizedRow;
import io.github.seiya_matsuoka.csv_to_insert_generator.validation.ErrorCollector;
import io.github.seiya_matsuoka.csv_to_insert_generator.validation.ValidationError;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * トークン化済みのCSV（TokenizedCsv）に対して、列の型（ColumnType）に沿って妥当性の検証を行う。
 *
 * <ul>
 *   <li>NULL / DEFAULT は全型で許可
 *   <li>EMPTY_STRING は text のみ許可（それ以外はエラー）
 *   <li>RAW のみ型別に検証する
 * </ul>
 */
public final class TypeValidator {

  private static final Pattern DECIMAL_PATTERN = Pattern.compile("^[+-]?(\\d+)(\\.\\d+)?$");

  private static final DateTimeFormatter TS_SPACE =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  private static final DateTimeFormatter TS_T =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

  /**
   * 型検証を行う。
   *
   * @param tokenized トークン化済みCSV
   * @return 検証結果（成功/失敗）
   * @throws NullPointerException tokenized が null の場合
   * @throws IllegalArgumentException types数とheaders数が一致しない場合（想定外の入力）
   */
  public TypeValidationResult validate(TokenizedCsv tokenized) {
    Objects.requireNonNull(tokenized, "tokenized is required");

    // CSVパース工程で types数とheader数は一致している想定だが、
    // 想定外の入力が流入した場合、以降の工程でバグの原因になるためチェックする。
    if (tokenized.types().size() != tokenized.headers().size()) {
      throw new IllegalArgumentException(
          "#typesの列数とヘッダの列数が一致していません: types="
              + tokenized.types().size()
              + ", headers="
              + tokenized.headers().size());
    }

    // 上限（ErrorCollector）を超えるまでエラーはできるだけ集める。
    ErrorCollector collector = new ErrorCollector();

    // 以後、各行でこの列数を前提にループする
    int columnCount = tokenized.headers().size();

    // 4行目以降のデータ行を順に処理する（行→列の順に走査し、RAWトークンを中心に検証する）
    for (TokenizedRow row : tokenized.rows()) {
      for (int i = 0; i < columnCount; i++) {
        // 列名（header）と型（types）とデータ行（row）を同じインデックスで参照する
        String columnName = tokenized.headers().get(i);
        ColumnType type = tokenized.types().get(i);
        ValueToken token = row.tokenAt(i);

        validateCell(row.fileLine(), columnName, type, token, collector);

        if (collector.isTruncated()) {
          break;
        }
      }
      if (collector.isTruncated()) {
        break;
      }
    }

    // 失敗時：エラーが1件でもあれば失敗として返す（TokenizedCsvは返さない）
    // 成功時のみ TokenizedCsv を返すことで、後工程は成功前提で組みやすくする。
    if (collector.hasErrors()) {
      return TypeValidationResult.failure(collector.errors(), collector.isTruncated());
    }

    // 成功時：後工程（SQL生成）にそのまま渡せるよう tokenized を返す
    return TypeValidationResult.success(tokenized);
  }

  /**
   * 1セル分の値（ValueToken）を型に沿って検証する。
   *
   * @param fileLine ファイル先頭からの行番号（1始まり）
   * @param columnName 列名
   * @param type 列の型
   * @param token 値トークン
   * @param collector エラー収集器
   * @throws NullPointerException columnName/type/token/collector が null の場合
   */
  private void validateCell(
      int fileLine,
      String columnName,
      ColumnType type,
      ValueToken token,
      ErrorCollector collector) {
    Objects.requireNonNull(columnName, "columnName is required");
    Objects.requireNonNull(type, "type is required");
    Objects.requireNonNull(token, "token is required");
    Objects.requireNonNull(collector, "collector is required");

    // NULL / DEFAULT は全型で許可
    if (token.kind() == ValueToken.Kind.NULL || token.kind() == ValueToken.Kind.DEFAULT) {
      return;
    }

    // EMPTY_STRING は text のみ許可（トークン化の処理で弾いている想定だが防御的にチェック）
    if (token.kind() == ValueToken.Kind.EMPTY_STRING) {
      if (type != ColumnType.TEXT) {
        collector.add(
            new ValidationError(
                fileLine, columnName, type.id(), token.original(), "text型以外では空文字は使用できません"));
      }
      return;
    }

    // RAW（通常の値）以外はここでは存在しない想定だが、安全策で未知のKindが混入した場合はここで弾く（通常は発生しない）
    if (token.kind() != ValueToken.Kind.RAW) {
      collector.add(
          new ValidationError(
              fileLine, columnName, type.id(), token.original(), "未知の値トークンです: " + token.kind()));
      return;
    }

    // 入力ミスで前後空白が混ざった場合に備えて、検証では trim した値で判定する（エラー表示用に token はそのまま残す）
    String s = token.value().orElse("").trim();

    switch (type) {
      case TEXT -> {
        // text は RAW の内容を制限しないため、検証しない（SQL生成側でエスケープする）
      }
      case INT -> validateInt(fileLine, columnName, type, token, s, collector);
      case DECIMAL -> validateDecimal(fileLine, columnName, type, token, s, collector);
      case BOOL -> validateBool(fileLine, columnName, type, token, s, collector);
      case DATE -> validateDate(fileLine, columnName, type, token, s, collector);
      case TIMESTAMP -> validateTimestamp(fileLine, columnName, type, token, s, collector);
      case UUID -> validateUuid(fileLine, columnName, type, token, s, collector);
    }
  }

  private void validateInt(
      int fileLine,
      String columnName,
      ColumnType type,
      ValueToken token,
      String s,
      ErrorCollector collector) {
    try {
      Integer.parseInt(s);
    } catch (NumberFormatException e) {
      collector.add(
          new ValidationError(
              fileLine, columnName, type.id(), token.original(), "intとして解釈できません（例: 0, 123, -10）"));
    }
  }

  private void validateDecimal(
      int fileLine,
      String columnName,
      ColumnType type,
      ValueToken token,
      String s,
      ErrorCollector collector) {
    // 指数表記（1e3など）は許可しない方針
    if (!DECIMAL_PATTERN.matcher(s).matches()) {
      collector.add(
          new ValidationError(
              fileLine,
              columnName,
              type.id(),
              token.original(),
              "decimalとして解釈できません（例: 0, 12.34, -0.5）"));
      return;
    }
    try {
      new BigDecimal(s);
    } catch (NumberFormatException e) {
      collector.add(
          new ValidationError(
              fileLine, columnName, type.id(), token.original(), "decimalとして解釈できません"));
    }
  }

  private void validateBool(
      int fileLine,
      String columnName,
      ColumnType type,
      ValueToken token,
      String s,
      ErrorCollector collector) {
    String lower = s.toLowerCase();
    if (!lower.equals("true") && !lower.equals("false")) {
      collector.add(
          new ValidationError(
              fileLine, columnName, type.id(), token.original(), "boolとして解釈できません（true/falseのみ）"));
    }
  }

  private void validateDate(
      int fileLine,
      String columnName,
      ColumnType type,
      ValueToken token,
      String s,
      ErrorCollector collector) {
    try {
      LocalDate.parse(s); // yyyy-MM-dd
    } catch (DateTimeParseException e) {
      collector.add(
          new ValidationError(
              fileLine, columnName, type.id(), token.original(), "dateとして解釈できません（yyyy-MM-dd）"));
    }
  }

  private void validateTimestamp(
      int fileLine,
      String columnName,
      ColumnType type,
      ValueToken token,
      String s,
      ErrorCollector collector) {
    // 2種類のフォーマットを許可: "yyyy-MM-dd HH:mm:ss" / "yyyy-MM-dd'T'HH:mm:ss"
    if (canParseLocalDateTime(s, TS_SPACE) || canParseLocalDateTime(s, TS_T)) {
      return;
    }
    collector.add(
        new ValidationError(
            fileLine,
            columnName,
            type.id(),
            token.original(),
            "timestampとして解釈できません（yyyy-MM-dd HH:mm:ss もしくは yyyy-MM-dd'T'HH:mm:ss）"));
  }

  private boolean canParseLocalDateTime(String s, DateTimeFormatter fmt) {
    try {
      LocalDateTime.parse(s, fmt);
      return true;
    } catch (DateTimeParseException e) {
      return false;
    }
  }

  private void validateUuid(
      int fileLine,
      String columnName,
      ColumnType type,
      ValueToken token,
      String s,
      ErrorCollector collector) {
    try {
      UUID.fromString(s);
    } catch (IllegalArgumentException e) {
      collector.add(
          new ValidationError(
              fileLine,
              columnName,
              type.id(),
              token.original(),
              "uuidとして解釈できません（例: 550e8400-e29b-41d4-a716-446655440000）"));
    }
  }
}
