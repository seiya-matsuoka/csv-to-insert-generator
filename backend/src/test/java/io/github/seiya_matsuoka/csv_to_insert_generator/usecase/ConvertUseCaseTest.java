package io.github.seiya_matsuoka.csv_to_insert_generator.usecase;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.seiya_matsuoka.csv_to_insert_generator.csv.CsvFormatDParser;
import io.github.seiya_matsuoka.csv_to_insert_generator.sql.InsertSqlGenerator;
import io.github.seiya_matsuoka.csv_to_insert_generator.tokenize.ValueTokenizer;
import io.github.seiya_matsuoka.csv_to_insert_generator.validator.TypeValidator;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

/** {@link ConvertUseCase} のテスト。 */
public class ConvertUseCaseTest {

  // 正常なCSVを渡すと成功し、SQL本文と出力ファイル名が得られることを確認
  @Test
  void shouldReturnSqlAndFileName_whenValidCsvIsGiven() {

    String csv =
        "#table=users\n"
            + "#types=int,text,bool,date,timestamp,uuid\n"
            + "id,name,is_active,birthday,created_at,user_uuid\n"
            + "1,Alice,true,1990-01-02,2026-01-05 10:00:00,550e8400-e29b-41d4-a716-446655440000\n";

    // 生成日時を固定して、ファイル名のアサートを安定させる
    Clock fixed = Clock.fixed(Instant.parse("2026-01-21T09:30:00Z"), ZoneId.of("Asia/Tokyo"));

    ConvertUseCase useCase =
        new ConvertUseCase(
            new CsvFormatDParser(),
            new ValueTokenizer(),
            new TypeValidator(),
            new InsertSqlGenerator(),
            fixed);

    ConvertResult result = useCase.convert(new ConvertRequest(csv, "input.csv"));

    assertTrue(result.isOk());
    assertTrue(result.sqlText().isPresent());
    assertTrue(result.outputFileName().isPresent());

    // BEGIN/COMMITが含まれること（SQL生成まで到達している証拠）
    String sql = result.sqlText().orElseThrow();
    assertTrue(sql.contains("BEGIN;"));
    assertTrue(sql.contains("COMMIT;"));

    // 出力ファイル名形式（insert_<table>_<timestamp>.sql）
    String fileName = result.outputFileName().orElseThrow();
    assertTrue(fileName.startsWith("insert_users_"));
    assertTrue(fileName.endsWith(".sql"));
  }

  // 型に合わない値（int列にabc）を渡すと失敗し、errorsが返ることを確認
  @Test
  void shouldReturnErrors_whenInvalidValueForTypeIsGiven() {

    String csv = "#table=users\n" + "#types=int\n" + "id\n" + "abc\n";

    ConvertUseCase useCase = new ConvertUseCase();
    ConvertResult result = useCase.convert(new ConvertRequest(csv, "bad.csv"));

    assertFalse(result.isOk());
    assertFalse(result.errors().isEmpty());
  }
}
