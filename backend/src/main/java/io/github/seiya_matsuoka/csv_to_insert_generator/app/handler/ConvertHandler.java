package io.github.seiya_matsuoka.csv_to_insert_generator.app.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import io.github.seiya_matsuoka.csv_to_insert_generator.api.dto.ConvertFailureDto;
import io.github.seiya_matsuoka.csv_to_insert_generator.api.dto.ConvertSuccessDto;
import io.github.seiya_matsuoka.csv_to_insert_generator.api.dto.ErrorDto;
import io.github.seiya_matsuoka.csv_to_insert_generator.app.HttpResponses;
import io.github.seiya_matsuoka.csv_to_insert_generator.app.RouteHandler;
import io.github.seiya_matsuoka.csv_to_insert_generator.app.multipart.MultipartCsvExtractor;
import io.github.seiya_matsuoka.csv_to_insert_generator.usecase.ConvertRequest;
import io.github.seiya_matsuoka.csv_to_insert_generator.usecase.ConvertResult;
import io.github.seiya_matsuoka.csv_to_insert_generator.usecase.ConvertUseCase;
import io.github.seiya_matsuoka.csv_to_insert_generator.validation.ErrorCollector;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

/**
 * POST /convert を処理するハンドラ。
 *
 * <p>責務：
 *
 * <ul>
 *   <li>multipart/form-data を解析して入力（table + csv）を取り出す
 *   <li>CSV本文を整形（#table 行を table に揃える）
 *   <li>ConvertUseCase を呼び出して結果を JSON DTO で返す
 * </ul>
 *
 * <p>注意：
 *
 * <ul>
 *   <li>ビジネスロジックは ConvertUseCase 側に寄せる（ここでは入出力と整形のみ）
 *   <li>想定外例外もできるだけ JSON で返す（Routerの500テキストに落ちないようにする）
 * </ul>
 */
public final class ConvertHandler implements RouteHandler {

  /** generatedAt の出力フォーマット（API固定）。 */
  private static final DateTimeFormatter GENERATED_AT_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  /** multipart の file パートに元ファイル名が無い場合の代替名。 */
  private static final String DEFAULT_INPUT_FILE_NAME = "upload.csv";

  private final ConvertUseCase useCase;
  private final MultipartCsvExtractor extractor;
  private final ObjectMapper objectMapper;

  /**
   * コンストラクタ。
   *
   * @param useCase 変換ユースケース
   * @param extractor multipart 抽出器（table + csv）
   * @param objectMapper JSON 変換用
   * @throws NullPointerException 引数がnullの場合
   */
  public ConvertHandler(
      ConvertUseCase useCase, MultipartCsvExtractor extractor, ObjectMapper objectMapper) {
    this.useCase = Objects.requireNonNull(useCase, "useCase is required");
    this.extractor = Objects.requireNonNull(extractor, "extractor is required");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper is required");
  }

  @Override
  public void handle(HttpExchange exchange) throws Exception {
    Objects.requireNonNull(exchange, "exchange is required");

    try {
      // 1) multipart から table と CSV(bytes) を抽出
      MultipartCsvExtractor.ExtractedCsv extracted = extractor.extract(exchange);

      // 2) bytes -> text（UTF-8固定）。BOM が入っているケースもあるので除去する
      String csvText = new String(extracted.csvBytes(), StandardCharsets.UTF_8);
      csvText = stripUtf8Bom(csvText);

      // 3) table はフォーム入力を正とする（CSV先頭の #table 行を差し替える/無ければ前置する）
      csvText = enforceTableMetaLine(csvText, extracted.tableName());

      // 4) usecase に渡す入力ファイル名（ヘッダコメント用）
      String inputFileName =
          (extracted.originalFileName() == null || extracted.originalFileName().isBlank())
              ? DEFAULT_INPUT_FILE_NAME
              : extracted.originalFileName();

      // 5) ユースケース実行
      ConvertRequest request = new ConvertRequest(csvText, inputFileName);
      ConvertResult result = useCase.convert(request);

      // 6) 結果をDTOへ変換し JSON で返す
      if (result.isOk()) {
        String generatedAt =
            result
                .generatedAt()
                .map(GENERATED_AT_FMT::format)
                .orElseGet(() -> GENERATED_AT_FMT.format(LocalDateTime.now()));

        String sqlText = result.sqlText().orElseThrow();
        String outputFileName = result.outputFileName().orElseThrow();

        ConvertSuccessDto dto = new ConvertSuccessDto(generatedAt, outputFileName, sqlText);
        sendJson(exchange, 200, dto);
        return;
      }

      // 変換は実行したが、バリデーション等で失敗（422）
      String generatedAt = GENERATED_AT_FMT.format(LocalDateTime.now());
      List<ErrorDto> errors = result.errors().stream().map(ErrorDto::from).toList();

      ConvertFailureDto dto =
          new ConvertFailureDto(
              generatedAt, errors, result.isTruncated(), ErrorCollector.DEFAULT_MAX_ERRORS);

      sendJson(exchange, 422, dto);

    } catch (IllegalArgumentException e) {
      // リクエスト不正（multipartでない、必須欠落、サイズ超過、table不正など）
      String generatedAt = GENERATED_AT_FMT.format(LocalDateTime.now());

      ConvertFailureDto dto =
          new ConvertFailureDto(
              generatedAt,
              List.of(
                  new ErrorDto(
                      0,
                      "(request)",
                      "(request)",
                      "",
                      Objects.toString(e.getMessage(), "Bad Request"))),
              false,
              ErrorCollector.DEFAULT_MAX_ERRORS);

      sendJson(exchange, 400, dto);

    } catch (Exception e) {
      // 想定外（500）。Routerに落とすと text/plain になってしまうのでここでJSON化する。
      String generatedAt = GENERATED_AT_FMT.format(LocalDateTime.now());

      ConvertFailureDto dto =
          new ConvertFailureDto(
              generatedAt,
              List.of(new ErrorDto(0, "(system)", "(system)", "", "Internal Server Error")),
              false,
              ErrorCollector.DEFAULT_MAX_ERRORS);

      sendJson(exchange, 500, dto);
    }
  }

  /**
   * DTOをJSONにして送信する。
   *
   * @param exchange HttpExchange
   * @param status HTTPステータス
   * @param dto レスポンスDTO
   * @throws Exception JSON生成/送信に失敗した場合
   */
  private void sendJson(HttpExchange exchange, int status, Object dto) throws Exception {
    byte[] json = objectMapper.writeValueAsBytes(dto);
    HttpResponses.sendJson(exchange, status, json);
  }

  /**
   * UTF-8 BOM（\uFEFF）が先頭に付いている場合に除去する。
   *
   * @param text 入力テキスト
   * @return BOM除去後テキスト
   */
  private String stripUtf8Bom(String text) {
    if (text == null || text.isEmpty()) {
      return "";
    }
    if (text.charAt(0) == '\uFEFF') {
      return text.substring(1);
    }
    return text;
  }

  /**
   * CSV先頭の #table 行を tableName に揃える。
   *
   * <p>仕様上「#table=...」は必須だが、UI側で省略したCSVを上げても動くように、 無ければ先頭に追加する。
   *
   * @param csvText CSV本文
   * @param tableName テーブル名（フォーム入力）
   * @return #table 行を揃えたCSV本文
   */
  private String enforceTableMetaLine(String csvText, String tableName) {
    String header = "#table=" + tableName;

    if (csvText == null || csvText.isBlank()) {
      return header + "\n";
    }

    // 先頭行を取り出し、#table= なら差し替え、違うなら前置する
    int lf = csvText.indexOf('\n');
    String firstLine = (lf >= 0) ? csvText.substring(0, lf) : csvText;

    if (firstLine.startsWith("#table=")) {
      // 改行コードは元に合わせる（\r\n を含む先頭行の場合もあるため、先頭行の末尾だけを置換する）
      if (lf >= 0) {
        return header + csvText.substring(lf);
      }
      return header;
    }

    // #table 行が無い場合は先頭に追加
    return header + "\n" + csvText;
  }
}
