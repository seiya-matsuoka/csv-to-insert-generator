package io.github.seiya_matsuoka.csv_to_insert_generator.usecase;

import io.github.seiya_matsuoka.csv_to_insert_generator.validation.ValidationError;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * ConvertUseCaseの結果（成功/失敗）を表す。
 *
 * <p>成功時: sqlText / outputFileName / generatedAt が存在し、errors は空。
 *
 * <p>失敗時: errors が存在し、sqlText / outputFileName / generatedAt は空。
 */
public final class ConvertResult {

  private final String sqlText; // successのみ
  private final String outputFileName; // successのみ
  private final LocalDateTime generatedAt; // successのみ

  private final List<ValidationError> errors;
  private final boolean truncated;

  private ConvertResult(
      String sqlText,
      String outputFileName,
      LocalDateTime generatedAt,
      List<ValidationError> errors,
      boolean truncated) {
    this.sqlText = sqlText;
    this.outputFileName = outputFileName;
    this.generatedAt = generatedAt;
    this.errors = List.copyOf(Objects.requireNonNull(errors, "errors is required"));
    this.truncated = truncated;
  }

  /**
   * 成功結果を生成する。
   *
   * @param sqlText 生成SQL本文
   * @param outputFileName 出力ファイル名（insert_<table>_<timestamp>.sql）
   * @param generatedAt 生成日時
   * @return 成功結果
   * @throws NullPointerException 引数がnullの場合
   */
  public static ConvertResult success(
      String sqlText, String outputFileName, LocalDateTime generatedAt) {
    Objects.requireNonNull(sqlText, "sqlText is required");
    Objects.requireNonNull(outputFileName, "outputFileName is required");
    Objects.requireNonNull(generatedAt, "generatedAt is required");
    return new ConvertResult(sqlText, outputFileName, generatedAt, List.of(), false);
  }

  /**
   * 失敗結果を生成する。
   *
   * @param errors エラー一覧
   * @param truncated エラー打ち切りが発生したか
   * @return 失敗結果
   * @throws NullPointerException errorsがnullの場合
   */
  public static ConvertResult failure(List<ValidationError> errors, boolean truncated) {
    return new ConvertResult(null, null, null, errors, truncated);
  }

  /**
   * 成功かどうかを返す。
   *
   * @return 成功ならtrue
   */
  public boolean isOk() {
    return sqlText != null && errors.isEmpty();
  }

  /**
   * 成功時のSQL本文を返す。
   *
   * @return SQL本文（失敗時はempty）
   */
  public Optional<String> sqlText() {
    return Optional.ofNullable(sqlText);
  }

  /**
   * 成功時の出力ファイル名を返す。
   *
   * @return 出力ファイル名（失敗時はempty）
   */
  public Optional<String> outputFileName() {
    return Optional.ofNullable(outputFileName);
  }

  /**
   * 成功時の生成日時を返す。
   *
   * @return 生成日時（失敗時はempty）
   */
  public Optional<LocalDateTime> generatedAt() {
    return Optional.ofNullable(generatedAt);
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
   * @return 打ち切りが発生した場合true
   */
  public boolean isTruncated() {
    return truncated;
  }
}
