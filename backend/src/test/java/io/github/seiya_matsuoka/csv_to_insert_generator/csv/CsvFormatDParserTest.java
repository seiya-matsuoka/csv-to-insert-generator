package io.github.seiya_matsuoka.csv_to_insert_generator.csv;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** {@link CsvFormatDParser} のテスト。 */
public class CsvFormatDParserTest {

  // フォーマットDの最小ケースがエラーなくパースできることを確認（#table/#types/header/data）
  @Test
  void shouldParseSuccessfully_whenValidFormatDIsGiven() {
    String csv =
        """
        #table=users
        #types=int,text,bool,date,timestamp,uuid
        id,name,is_active,birthday,created_at,user_uuid
        1,Alice,true,1990-01-02,2026-01-05 10:00:00,550e8400-e29b-41d4-a716-446655440000
        """;

    CsvFormatDParser parser = new CsvFormatDParser();
    CsvParseResult result = parser.parse(csv);

    assertTrue(result.isOk());
    assertTrue(result.errors().isEmpty());
  }

  // 1行目が #table=... でない場合にエラーになることを確認
  @Test
  void shouldReturnErrors_whenTableLineIsInvalid() {
    String csv =
        """
        #tables=users
        #types=int,text
        id,name
        1,Alice
        """;

    CsvFormatDParser parser = new CsvFormatDParser();
    CsvParseResult result = parser.parse(csv);

    assertFalse(result.isOk());
    assertFalse(result.errors().isEmpty());
  }

  // #types に未知の型が含まれる場合にエラーになることを確認
  @Test
  void shouldReturnErrors_whenUnknownTypeIsSpecified() {
    String csv =
        """
        #table=users
        #types=int,unknown
        id,name
        1,Alice
        """;

    CsvFormatDParser parser = new CsvFormatDParser();
    CsvParseResult result = parser.parse(csv);

    assertFalse(result.isOk());
    assertFalse(result.errors().isEmpty());
  }

  // #typesの個数とヘッダ列数が一致しない場合にエラーになることを確認
  @Test
  void shouldReturnErrors_whenTypesAndHeaderCountsMismatch() {
    String csv =
        """
        #table=users
        #types=int,text,bool
        id,name
        1,Alice
        """;

    CsvFormatDParser parser = new CsvFormatDParser();
    CsvParseResult result = parser.parse(csv);

    assertFalse(result.isOk());
    assertFalse(result.errors().isEmpty());
  }

  // ヘッダに列名重複がある場合にエラーになることを確認
  @Test
  void shouldReturnErrors_whenHeaderHasDuplicateColumns() {
    String csv =
        """
        #table=users
        #types=int,text
        id,id
        1,Alice
        """;

    CsvFormatDParser parser = new CsvFormatDParser();
    CsvParseResult result = parser.parse(csv);

    assertFalse(result.isOk());
    assertFalse(result.errors().isEmpty());
  }

  // データ行の列数がヘッダと一致しない場合にエラーになることを確認（列数チェック）
  @Test
  void shouldReturnErrors_whenDataRowColumnCountMismatch() {
    String csv =
        """
        #table=users
        #types=int,text
        id,name
        1,Alice,EXTRA
        """;

    CsvFormatDParser parser = new CsvFormatDParser();
    CsvParseResult result = parser.parse(csv);

    assertFalse(result.isOk());
    assertFalse(result.errors().isEmpty());
  }

  // 先頭にBOMが付いていても #table 判定ができることを確認（BOM除去が効いている）
  @Test
  void shouldStripUtf8Bom_whenCsvStartsWithBom() {
    String csv = "\uFEFF#table=users\n" + "#types=int,text\n" + "id,name\n" + "1,Alice\n";

    CsvFormatDParser parser = new CsvFormatDParser();
    CsvParseResult result = parser.parse(csv);

    assertTrue(result.isOk());
  }
}
