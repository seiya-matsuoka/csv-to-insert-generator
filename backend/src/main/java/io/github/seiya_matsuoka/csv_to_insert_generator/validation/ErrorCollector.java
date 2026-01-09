package io.github.seiya_matsuoka.csv_to_insert_generator.validation;

import java.util.ArrayList;
import java.util.List;

/**
 * 検証エラーを収集するためのクラス。
 *
 * <ul>
 *   <li>できる限り全データ行を検証し、エラーを収集する
 *   <li>ただし、エラーは最大N件まで（初期は100件）
 *   <li>N件を超えた場合は収集を打ち切り、truncated=true を返す
 * </ul>
 */
public final class ErrorCollector {

  public static final int DEFAULT_MAX_ERRORS = 100;

  private final int maxErrors;
  private final List<ValidationError> errors = new ArrayList<>();
  private boolean truncated = false;

  /** デフォルト上限（100件）で作成する。 */
  public ErrorCollector() {
    this(DEFAULT_MAX_ERRORS);
  }

  /**
   * 指定上限で作成する。
   *
   * @param maxErrors 最大件数（1以上）
   */
  public ErrorCollector(int maxErrors) {
    if (maxErrors <= 0) {
      throw new IllegalArgumentException("maxErrors は1以上でなければいけません: " + maxErrors);
    }
    this.maxErrors = maxErrors;
  }

  /** エラーを追加する。 上限を超えた場合は保持せず、truncated を true にする。 */
  public void add(ValidationError error) {
    if (errors.size() < maxErrors) {
      errors.add(error);
    } else {
      // 上限超過が発生したら truncated を true にする（以降は何回呼ばれてもtrueのまま）
      truncated = true;
    }
  }

  /** 収集済みエラーを返す（不変List）。 */
  public List<ValidationError> errors() {
    return List.copyOf(errors);
  }

  /** エラーが1件でもあるか。 */
  public boolean hasErrors() {
    return !errors.isEmpty();
  }

  /** 収集件数（上限以内）。 */
  public int size() {
    return errors.size();
  }

  /** 上限を超えるエラーが存在したか（打ち切り発生）。 */
  public boolean isTruncated() {
    return truncated;
  }

  /** 最大収集件数。 */
  public int maxErrors() {
    return maxErrors;
  }
}
