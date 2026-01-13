package io.github.seiya_matsuoka.csv_to_insert_generator.csv;

/**
 * 文字列先頭の UTF-8 BOM（U+FEFF）を取り除くユーティリティ。
 *
 * <p>CSVの先頭にBOMが付いていると、1セル目の先頭に不可視文字が混ざり、フォーマット判定が失敗する原因になるため除去する。
 */
public final class BomUtil {

  private static final char UTF8_BOM = '\uFEFF';

  private BomUtil() {}

  /**
   * 先頭がBOMであれば除去して返す。
   *
   * @param text 入力文字列（null可）
   * @return BOM除去後の文字列（nullはそのまま返す）
   */
  public static String stripUtf8Bom(String text) {
    if (text == null || text.isEmpty()) {
      return text;
    }
    if (text.charAt(0) == UTF8_BOM) {
      return text.substring(1);
    }
    return text;
  }
}
