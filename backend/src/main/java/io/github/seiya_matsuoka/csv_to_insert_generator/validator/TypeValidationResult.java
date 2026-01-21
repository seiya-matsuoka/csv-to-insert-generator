package io.github.seiya_matsuoka.csv_to_insert_generator.validator;

import io.github.seiya_matsuoka.csv_to_insert_generator.tokenize.TokenizedCsv;
import io.github.seiya_matsuoka.csv_to_insert_generator.validation.ValidationError;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 型検証（TypeValidator）の結果（成功/失敗）を表す。
 *
 * <p>成功時: tokenized が存在し、errors は空。 失敗時: tokenized は空で、errors にValidationErrorが入る。
 */
public final class TypeValidationResult {

  private final TokenizedCsv tokenized; // 成功時のみ非null
  private final List<ValidationError> errors;
  private final boolean truncated;

  private TypeValidationResult(
      TokenizedCsv tokenized, List<ValidationError> errors, boolean truncated) {
    this.tokenized = tokenized;
    this.errors = List.copyOf(Objects.requireNonNull(errors, "errors is required"));
    this.truncated = truncated;
  }

  /**
   * 成功結果を生成する。
   *
   * @param tokenized tokenizedCsv（トークン化済み）
   * @return 成功結果
   * @throws NullPointerException tokenized が null の場合
   */
  public static TypeValidationResult success(TokenizedCsv tokenized) {
    return new TypeValidationResult(
        Objects.requireNonNull(tokenized, "tokenized is required"), List.of(), false);
  }

  /**
   * 失敗結果を生成する。
   *
   * @param errors エラー一覧
   * @param truncated エラー打ち切りが発生したか
   * @return 失敗結果
   * @throws NullPointerException errors が null の場合
   */
  public static TypeValidationResult failure(List<ValidationError> errors, boolean truncated) {
    return new TypeValidationResult(null, errors, truncated);
  }

  /**
   * 成功かどうか。
   *
   * @return 成功ならtrue
   */
  public boolean isOk() {
    return tokenized != null && errors.isEmpty();
  }

  /**
   * 成功時の tokenizedCsv を返す。
   *
   * @return tokenizedCsv（失敗時はempty）
   */
  public Optional<TokenizedCsv> tokenized() {
    return Optional.ofNullable(tokenized);
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
