package io.github.seiya_matsuoka.csv_to_insert_generator.domain;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * CSVの #types 行で指定される「固定の型セット」。
 *
 * <p>扱える型は固定（text/int/...）のため、未知の型はエラーとする。
 *
 * <p>そのため、取り扱う値が限定されたドメインを表現しやすい enum を採用する。
 *
 * <p>入力上は小文字を想定するが、実装側では trim + lower-case で正規化して扱う。
 */
public enum ColumnType {
  TEXT("text"),
  INT("int"),
  DECIMAL("decimal"),
  BOOL("bool"),
  DATE("date"),
  TIMESTAMP("timestamp"),
  UUID("uuid");

  private final String id;

  ColumnType(String id) {
    this.id = id;
  }

  /**
   * CSV上で指定する型名（例: {@code "int"}）を返す。
   *
   * @return 型名（固定の識別子）
   */
  public String id() {
    return id;
  }

  /**
   * 文字列から型を解決する。未知の型の場合は {@link Optional#empty()} を返す。
   *
   * <p>入力は {@code trim()} した上で小文字に正規化して比較する。
   *
   * @param raw CSVに書かれた型名（例: {@code "INT"} や {@code " int "} なども許容）
   * @return 解決結果（未知の型の場合は empty）
   */
  public static Optional<ColumnType> fromId(String raw) {
    if (raw == null) {
      return Optional.empty();
    }

    String normalized = raw.trim().toLowerCase(Locale.ROOT);
    for (ColumnType t : values()) {
      if (t.id.equals(normalized)) {
        return Optional.of(t);
      }
    }
    return Optional.empty();
  }

  /**
   * 文字列から型を解決する。未知の型の場合は {@link IllegalArgumentException} を投げる。
   *
   * @param raw CSVに書かれた型名（null不可）
   * @return 解決された型
   * @throws NullPointerException rawがnullの場合
   * @throws IllegalArgumentException 未知の型の場合
   */
  public static ColumnType requireFromId(String raw) {
    Objects.requireNonNull(raw, "type is required");
    return fromId(raw).orElseThrow(() -> new IllegalArgumentException("未知の型です: " + raw));
  }
}
