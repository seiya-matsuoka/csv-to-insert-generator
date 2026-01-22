package io.github.seiya_matsuoka.csv_to_insert_generator.sql;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.seiya_matsuoka.csv_to_insert_generator.domain.ColumnType;
import io.github.seiya_matsuoka.csv_to_insert_generator.domain.ValueToken;
import io.github.seiya_matsuoka.csv_to_insert_generator.tokenize.TokenizedCsv;
import io.github.seiya_matsuoka.csv_to_insert_generator.tokenize.TokenizedRow;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@link InsertSqlGenerator} のテスト。
 *
 * <p>生成されるSQLの形が重要なので、スナップショット寄りに主要文字列を含むか確認する。
 */
public class InsertSqlGeneratorTest {

  // - ヘッダコメント、BEGIN/COMMIT、INSERT文が含まれることを確認
  // - textのシングルクォートがエスケープされることを確認
  @Test
  void shouldGenerateSqlWithHeaderAndBeginCommit_whenValidTokenizedCsvIsGiven() {

    TokenizedCsv csv =
        new TokenizedCsv(
            "users",
            List.of(ColumnType.INT, ColumnType.TEXT, ColumnType.BOOL),
            List.of("id", "name", "is_active"),
            List.of(
                new TokenizedRow(
                    4,
                    List.of(
                        ValueToken.ofRaw("1"),
                        ValueToken.ofRaw("O'Reilly"),
                        ValueToken.ofRaw("true"))),
                new TokenizedRow(
                    5,
                    List.of(
                        ValueToken.ofNull(""),
                        ValueToken.ofEmptyString("\"\""),
                        ValueToken.ofDefault("DEFAULT")))));

    InsertSqlGenerator generator = new InsertSqlGenerator();
    String sql = generator.generate(csv, "input.csv", LocalDateTime.of(2026, 1, 21, 18, 30, 0));

    assertTrue(sql.contains("-- table: users"));
    assertTrue(sql.contains("-- input: input.csv"));
    assertTrue(sql.contains("-- rows: 2"));
    assertTrue(sql.contains("BEGIN;"));
    assertTrue(sql.contains("COMMIT;"));

    // 1行目INSERTの検証
    // 基本形と、textのシングルクォートがエスケープされること
    assertTrue(
        sql.contains("INSERT INTO users (id, name, is_active) VALUES (1, 'O''Reilly', TRUE);"));

    // 2行目INSERTの検証
    // NULL / 空文字 / DEFAULT がVALUESに出ること（id=NULL, name='', is_active=DEFAULT）
    assertTrue(sql.contains("INSERT INTO users (id, name, is_active) VALUES (NULL, '', DEFAULT);"));
  }
}
