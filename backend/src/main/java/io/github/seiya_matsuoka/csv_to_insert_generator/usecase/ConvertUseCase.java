package io.github.seiya_matsuoka.csv_to_insert_generator.usecase;

import io.github.seiya_matsuoka.csv_to_insert_generator.csv.CsvFormatDParser;
import io.github.seiya_matsuoka.csv_to_insert_generator.csv.CsvParseResult;
import io.github.seiya_matsuoka.csv_to_insert_generator.csv.ParsedCsv;
import io.github.seiya_matsuoka.csv_to_insert_generator.sql.InsertSqlGenerator;
import io.github.seiya_matsuoka.csv_to_insert_generator.tokenize.TokenizedCsv;
import io.github.seiya_matsuoka.csv_to_insert_generator.tokenize.ValueTokenizationResult;
import io.github.seiya_matsuoka.csv_to_insert_generator.tokenize.ValueTokenizer;
import io.github.seiya_matsuoka.csv_to_insert_generator.validation.ValidationError;
import io.github.seiya_matsuoka.csv_to_insert_generator.validator.TypeValidationResult;
import io.github.seiya_matsuoka.csv_to_insert_generator.validator.TypeValidator;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

/**
 * CSV を受け取り、INSERT SQL を生成するユースケース。
 *
 * <p>統合する処理フロー:
 *
 * <ol>
 *   <li>CSVパース（メタ行/ヘッダ/列数）
 *   <li>値の解釈（NULL/DEFAULT/空文字/RAW）
 *   <li>型検証（int/decimal/bool/date/timestamp/uuid/text）
 *   <li>INSERT SQL生成（ヘッダコメント + BEGIN/COMMIT）
 * </ol>
 *
 * <p>出力ファイル名は {@code insert_<table>_<timestamp>.sql} の固定形式で生成する。
 */
public final class ConvertUseCase {

  private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

  private final CsvFormatDParser parser;
  private final ValueTokenizer tokenizer;
  private final TypeValidator typeValidator;
  private final InsertSqlGenerator sqlGenerator;
  private final Clock clock;

  /** デフォルト構成（本番利用）。 */
  public ConvertUseCase() {
    this(
        new CsvFormatDParser(),
        new ValueTokenizer(),
        new TypeValidator(),
        new InsertSqlGenerator(),
        Clock.systemDefaultZone());
  }

  /**
   * 依存注入用コンストラクタ（テストや拡張のため）。
   *
   * @param parser CSVパーサ
   * @param tokenizer 値トークナイザ
   * @param typeValidator 型検証器
   * @param sqlGenerator SQL生成器
   * @param clock 生成日時決定用のクロック（固定可能）
   * @throws NullPointerException 引数がnullの場合
   */
  public ConvertUseCase(
      CsvFormatDParser parser,
      ValueTokenizer tokenizer,
      TypeValidator typeValidator,
      InsertSqlGenerator sqlGenerator,
      Clock clock) {
    this.parser = Objects.requireNonNull(parser, "parser is required");
    this.tokenizer = Objects.requireNonNull(tokenizer, "tokenizer is required");
    this.typeValidator = Objects.requireNonNull(typeValidator, "typeValidator is required");
    this.sqlGenerator = Objects.requireNonNull(sqlGenerator, "sqlGenerator is required");
    this.clock = Objects.requireNonNull(clock, "clock is required");
  }

  /**
   * 変換を実行する。
   *
   * @param request 入力
   * @return 成功時はSQL本文＋出力ファイル名、失敗時はエラー一覧
   * @throws NullPointerException requestがnullの場合
   */
  public ConvertResult convert(ConvertRequest request) {
    Objects.requireNonNull(request, "request is required");

    try {
      // ---- 1) CSVパース ----
      CsvParseResult parseResult = parser.parse(request.csvText());
      if (!parseResult.isOk()) {
        return ConvertResult.failure(parseResult.errors(), parseResult.isTruncated());
      }
      ParsedCsv parsed = parseResult.parsed().orElseThrow();

      // ---- 2) 値の解釈（NULL/DEFAULT/空文字/RAW）----
      ValueTokenizationResult tokenizationResult = tokenizer.tokenize(parsed);
      if (!tokenizationResult.isOk()) {
        return ConvertResult.failure(tokenizationResult.errors(), tokenizationResult.isTruncated());
      }
      TokenizedCsv tokenized = tokenizationResult.tokenized().orElseThrow();

      // ---- 3) 型検証 ----
      TypeValidationResult validationResult = typeValidator.validate(tokenized);
      if (!validationResult.isOk()) {
        return ConvertResult.failure(validationResult.errors(), validationResult.isTruncated());
      }
      TokenizedCsv validated = validationResult.tokenized().orElseThrow();

      // ---- 4) SQL生成（ヘッダコメント + BEGIN/COMMIT）----
      LocalDateTime generatedAt = LocalDateTime.ofInstant(clock.instant(), clock.getZone());

      // 出力ファイル名は固定形式：insert_<table>_<timestamp>.sql
      String outputFileName = buildOutputFileName(validated.tableName(), generatedAt);

      String sqlText = sqlGenerator.generate(validated, request.inputFileName(), generatedAt);

      return ConvertResult.success(sqlText, outputFileName, generatedAt);

    } catch (RuntimeException e) {
      // 想定外の例外はシステムエラーとして整形し、UIに返せる形にする（line番号は特定できないため 0 を使用（ファイル行とは別枠の意味））
      ValidationError err =
          new ValidationError(
              0,
              "(system)",
              "(system)",
              "",
              "予期しないエラーが発生しました: " + e.getClass().getSimpleName() + " - " + e.getMessage());
      return ConvertResult.failure(List.of(err), false);
    }
  }

  /**
   * 出力ファイル名 {@code insert_<table>_<timestamp>.sql} を生成する。
   *
   * <p>テーブル名はファイル名に安全な形へサニタイズする。
   *
   * @param tableName テーブル名
   * @param generatedAt 生成日時
   * @return 出力ファイル名
   */
  private String buildOutputFileName(String tableName, LocalDateTime generatedAt) {
    String safeTable = sanitizeForFileName(tableName);
    String ts = generatedAt.format(FILE_TS);
    return "insert_" + safeTable + "_" + ts + ".sql";
  }

  /**
   * ファイル名として安全に扱うため、英数字/アンダースコア/ハイフン以外をアンダースコアに置換する。
   *
   * @param s 入力文字列
   * @return サニタイズ後文字列
   */
  private String sanitizeForFileName(String s) {
    String in = (s == null || s.isBlank()) ? "table" : s.trim();
    return in.replaceAll("[^0-9A-Za-z_-]", "_");
  }
}
