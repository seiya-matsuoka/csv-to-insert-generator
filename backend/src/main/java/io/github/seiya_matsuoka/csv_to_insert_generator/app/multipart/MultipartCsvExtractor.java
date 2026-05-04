package io.github.seiya_matsuoka.csv_to_insert_generator.app.multipart;

import com.sun.net.httpserver.HttpExchange;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;

/**
 * multipart/form-data から、変換に必要な入力（table + csvファイル）を抽出するクラス。
 *
 * <p>責務： - リクエストがmultipartかどうかの判定 - フィールド名（table/file）を固定ルールで取り出す - 最小限のチェック（必須項目、サイズ上限、空チェック）
 *
 * <p>※CSVのパースや型検証はusecase/core側の責務（ここではやらない）
 */
public final class MultipartCsvExtractor {

  /** form-data のテーブル名フィールド名（固定） */
  public static final String FIELD_TABLE = "table";

  /** form-data のCSVファイルフィールド名（固定） */
  public static final String FIELD_FILE = "file";

  /** アップロード上限（リクエスト全体）。 ※必要なら後でenv化するが、まずは固定で軽量に進める。 */
  public static final long MAX_REQUEST_SIZE_BYTES = 2L * 1024L * 1024L; // 2MiB

  /** アップロード上限（ファイル単体） */
  public static final long MAX_FILE_SIZE_BYTES = 2L * 1024L * 1024L; // 2MiB

  private final FileUpload fileUpload;

  /** Extractorを生成します（FileUploadの設定もここで行います）。 */
  public MultipartCsvExtractor() {
    DiskFileItemFactory factory = new DiskFileItemFactory();
    FileUpload upload = new FileUpload(factory);

    // サイズ制限：意図せぬ巨大アップロードを防ぐ
    upload.setSizeMax(MAX_REQUEST_SIZE_BYTES);
    upload.setFileSizeMax(MAX_FILE_SIZE_BYTES);

    // ヘッダの文字コード（filename等）読み取り用
    upload.setHeaderEncoding(StandardCharsets.UTF_8.name());

    this.fileUpload = upload;
  }

  /**
   * HttpExchangeから table と CSVファイルを抽出します。
   *
   * @param exchange HttpExchange
   * @return 抽出結果
   * @throws IllegalArgumentException 入力が不正（multipartでない/必須欠落/サイズ超過など）
   */
  public ExtractedCsv extract(HttpExchange exchange) {
    Objects.requireNonNull(exchange, "exchange is required");

    HttpExchangeUploadContext ctx = new HttpExchangeUploadContext(exchange);

    // 1) multipartかどうかを先に判定（ここで弾くと分かりやすい）
    if (!FileUploadBase.isMultipartContent(ctx)) {
      throw new IllegalArgumentException("multipart/form-data のリクエストが必要です。");
    }

    try {
      // 2) multipartをパースして FileItem（フォーム項目/ファイル）に分解
      List<FileItem> items = fileUpload.parseRequest(ctx);

      String tableName = null;
      FileItem csvItem = null;

      // 3) 固定フィールド名で探索（table / file）
      for (FileItem item : items) {
        if (item.isFormField()) {
          if (FIELD_TABLE.equals(item.getFieldName())) {
            try {
              tableName = item.getString(StandardCharsets.UTF_8.name());
            } catch (java.io.UnsupportedEncodingException e) {
              // UTF-8 は通常サポートされるが、APIがチェック例外を要求するためここで握る
              throw new IllegalArgumentException("table フィールドの文字コード解析に失敗しました。", e);
            }
          }
          continue;
        }

        // fileパート
        if (FIELD_FILE.equals(item.getFieldName())) {
          csvItem = item;
        }
      }

      // 4) 必須チェック
      if (tableName == null || tableName.isBlank()) {
        throw new IllegalArgumentException("form-data の 'table' が未指定です。");
      }
      if (csvItem == null) {
        throw new IllegalArgumentException("form-data の 'file'（CSVファイル）が未指定です。");
      }

      tableName = tableName.trim();
      validateTableName(tableName);

      byte[] bytes = csvItem.get();
      if (bytes == null || bytes.length == 0) {
        throw new IllegalArgumentException("CSVファイルが空です。");
      }

      String originalFileName = normalizeFileName(csvItem.getName());

      return new ExtractedCsv(tableName, originalFileName, bytes);

    } catch (FileUploadBase.SizeLimitExceededException e) {
      throw new IllegalArgumentException("アップロードサイズが上限を超えています。", e);
    } catch (FileUploadException e) {
      throw new IllegalArgumentException("multipart の解析に失敗しました。", e);
    }
  }

  /**
   * 抽出結果（table + csv bytes）。
   *
   * @param tableName テーブル名
   * @param originalFileName 元のファイル名（不明ならnull）
   * @param csvBytes CSVの生bytes（UTF-8前提。BOM除去などは後段で実施）
   */
  public record ExtractedCsv(String tableName, String originalFileName, byte[] csvBytes) {
    public ExtractedCsv {
      Objects.requireNonNull(tableName, "tableName is required");
      Objects.requireNonNull(csvBytes, "csvBytes is required");
    }
  }

  /**
   * テーブル名の最小バリデーション。
   *
   * <p>本格的な引用符付き識別子などは扱わず、まずは安全な範囲に限定します。
   *
   * @param tableName テーブル名
   */
  private static void validateTableName(String tableName) {
    // 例：users, user_logs, USER01 はOK（小文字運用推奨だがここでは許容）
    // 例：users;drop table ... は弾く
    String t = tableName.trim();
    if (!t.matches("[A-Za-z_][A-Za-z0-9_]*")) {
      throw new IllegalArgumentException("テーブル名が不正です（英数字と_のみ、先頭は英字または_）。: " + tableName);
    }
  }

  /**
   * ブラウザから来るファイル名の正規化（path成分を落とす）。
   *
   * @param rawName raw filename
   * @return 正規化後ファイル名（空ならnull）
   */
  private static String normalizeFileName(String rawName) {
    if (rawName == null || rawName.isBlank()) {
      return null;
    }
    // Windows系ブラウザだと C:\fakepath\xxx.csv になることがある
    String n = rawName.replace("\\", "/");
    int idx = n.lastIndexOf('/');
    String base = (idx >= 0) ? n.substring(idx + 1) : n;
    base = base.trim();
    if (base.isEmpty()) {
      return null;
    }
    // 表示用なので軽く整形
    return base.toLowerCase(Locale.ROOT);
  }
}
