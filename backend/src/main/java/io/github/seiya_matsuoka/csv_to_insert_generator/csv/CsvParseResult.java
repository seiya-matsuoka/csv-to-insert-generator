package io.github.seiya_matsuoka.csv_to_insert_generator.csv;

import io.github.seiya_matsuoka.csv_to_insert_generator.validation.ValidationError;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * CSVパースの結果（成功/失敗）を表す。
 *
 * <p>成功時: parsed が存在し、errors は空。 失敗時: parsed は空で、errors にValidationErrorが入る。
 */
public final class CsvParseResult {

  private final ParsedCsv parsed; // 成功時のみ非null
  private final List<ValidationError> errors;
  private final boolean truncated;

  private CsvParseResult(ParsedCsv parsed, List<ValidationError> errors, boolean truncated) {
    this.parsed = parsed;
    this.errors = List.copyOf(Objects.requireNonNull(errors, "errors is required"));
    this.truncated = truncated;
  }

  /**
   * 成功結果を生成する。
   *
   * @param parsed パース結果
   * @return 成功結果
   * @throws NullPointerException parsedがnullの場合
   */
  public static CsvParseResult success(ParsedCsv parsed) {
    return new CsvParseResult(
        Objects.requireNonNull(parsed, "parsed is required"), List.of(), false);
  }

  /**
   * 失敗結果を生成する。
   *
   * @param errors エラー一覧
   * @param truncated エラー打ち切りが発生したか
   * @return 失敗結果
   * @throws NullPointerException errorsがnullの場合
   */
  public static CsvParseResult failure(List<ValidationError> errors, boolean truncated) {
    return new CsvParseResult(null, errors, truncated);
  }

  /**
   * 成功かどうか。
   *
   * @return 成功ならtrue
   */
  public boolean isOk() {
    return parsed != null && errors.isEmpty();
  }

  /**
   * 成功時のパース結果を返す。
   *
   * @return パース結果（失敗時はempty）
   */
  public Optional<ParsedCsv> parsed() {
    return Optional.ofNullable(parsed);
  }

  /**
   * エラー一覧を返す（成功時は空）。
   *
   * @return エラー一覧
   */
  public List<ValidationError> errors() {
    return errors;
  }

  /**
   * エラーの打ち切りが発生したか。
   *
   * @return 打ち切りが発生した場合 true
   */
  public boolean isTruncated() {
    return truncated;
  }
}
