package io.github.seiya_matsuoka.csv_to_insert_generator.usecase;

import java.util.Objects;

/**
 * 変換処理（CSV -> INSERT SQL）への入力を表す。
 *
 * <p>UI/HTTP層から渡される情報を、ユースケース層で扱いやすい形にまとめる。
 */
public final class ConvertRequest {

  private final String csvText;
  private final String inputFileName;

  /**
   * コンストラクタ。
   *
   * @param csvText CSVの本文
   * @param inputFileName 入力ファイル名（ヘッダコメントに出力する）
   * @throws NullPointerException 引数がnullの場合
   * @throws IllegalArgumentException csvTextが空の場合
   */
  public ConvertRequest(String csvText, String inputFileName) {
    this.csvText = Objects.requireNonNull(csvText, "csvText is required");
    this.inputFileName = Objects.requireNonNull(inputFileName, "inputFileName is required");

    if (this.csvText.isBlank()) {
      throw new IllegalArgumentException("csvTextが空白です");
    }
  }

  /**
   * CSV本文を返す。
   *
   * @return CSV本文
   */
  public String csvText() {
    return csvText;
  }

  /**
   * 入力ファイル名を返す。
   *
   * @return 入力ファイル名
   */
  public String inputFileName() {
    return inputFileName;
  }
}
