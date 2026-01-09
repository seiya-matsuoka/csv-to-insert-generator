package io.github.seiya_matsuoka.csv_to_insert_generator.validation;

import java.util.Objects;
import java.util.Optional;

/**
 * CSVセルの値を解釈した結果を表すトークン。
 *
 * <p>解釈ルール（NULL / DEFAULT / 空文字 / 生値）の結果を安全に受け渡すため。
 */
public final class ValueToken {

  /** 値の種別。 */
  public enum Kind {
    /** NULL を表す（空欄 or NULL）。 */
    NULL,
    /** DEFAULT を表す（DEFAULT）。 */
    DEFAULT,
    /** 空文字を表す（""）。text型のみ許可予定。 */
    EMPTY_STRING,
    /** 生値（通常の値）。 */
    RAW
  }

  private final Kind kind;
  private final String original;
  private final String value; // RAW/EMPTY_STRINGのみ有効。NULL/DEFAULTはnull。

  private ValueToken(Kind kind, String original, String value) {
    this.kind = Objects.requireNonNull(kind, "kind is required");
    this.original = Objects.requireNonNull(original, "original is required");
    this.value = value;
  }

  /** 種別を返す。 */
  public Kind kind() {
    return kind;
  }

  /** CSVセルに書かれていた元の文字列（そのまま）を返す。 エラー表示（input）に使用。 */
  public String original() {
    return original;
  }

  /** 値を返す（RAW/EMPTY_STRINGのみ）。 */
  public Optional<String> value() {
    return Optional.ofNullable(value);
  }

  /** NULLトークンを生成する。 */
  public static ValueToken ofNull(String original) {
    return new ValueToken(Kind.NULL, original, null);
  }

  /** DEFAULTトークンを生成する。 */
  public static ValueToken ofDefault(String original) {
    return new ValueToken(Kind.DEFAULT, original, null);
  }

  /** 空文字トークンを生成する。 */
  public static ValueToken ofEmptyString(String original) {
    // valueは空文字として保持（検証やSQL生成に使う）
    return new ValueToken(Kind.EMPTY_STRING, original, "");
  }

  /** 生値トークンを生成する。 */
  public static ValueToken ofRaw(String original) {
    return new ValueToken(Kind.RAW, original, original);
  }

  public boolean isNull() {
    return kind == Kind.NULL;
  }

  public boolean isDefault() {
    return kind == Kind.DEFAULT;
  }

  public boolean isEmptyString() {
    return kind == Kind.EMPTY_STRING;
  }

  public boolean isRaw() {
    return kind == Kind.RAW;
  }

  @Override
  public String toString() {
    return "ValueToken{kind=" + kind + ", original='" + original + "'}";
  }
}
