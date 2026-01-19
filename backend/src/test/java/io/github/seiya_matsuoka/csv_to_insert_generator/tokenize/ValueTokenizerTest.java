package io.github.seiya_matsuoka.csv_to_insert_generator.tokenize;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.seiya_matsuoka.csv_to_insert_generator.csv.CsvRow;
import io.github.seiya_matsuoka.csv_to_insert_generator.csv.ParsedCsv;
import io.github.seiya_matsuoka.csv_to_insert_generator.domain.ColumnType;
import io.github.seiya_matsuoka.csv_to_insert_generator.domain.ValueToken;
import java.util.List;
import org.junit.jupiter.api.Test;

/** {@link ValueTokenizer} のテスト。 */
public class ValueTokenizerTest {

  // 空欄（長さ0）のセルは NULL トークンになることを確認
  @Test
  void shouldTokenizeAsNull_whenCellIsEmpty() {

    ParsedCsv parsed =
        parsedOf(List.of(ColumnType.INT), List.of("id"), List.of(new CsvRow(4, List.of(""))));

    ValueTokenizer tokenizer = new ValueTokenizer();
    ValueTokenizationResult result = tokenizer.tokenize(parsed);

    assertTrue(result.isOk());
    ValueToken token = result.tokenized().orElseThrow().rows().get(0).tokenAt(0);
    assertEquals(ValueToken.Kind.NULL, token.kind());
  }

  // "NULL" は NULL トークンになることを確認
  @Test
  void shouldTokenizeAsNull_whenNullKeywordIsGiven() {

    ParsedCsv parsed =
        parsedOf(
            List.of(ColumnType.TEXT), List.of("name"), List.of(new CsvRow(4, List.of("NULL"))));

    ValueTokenizer tokenizer = new ValueTokenizer();
    ValueTokenizationResult result = tokenizer.tokenize(parsed);

    assertTrue(result.isOk());
    ValueToken token = result.tokenized().orElseThrow().rows().get(0).tokenAt(0);
    assertEquals(ValueToken.Kind.NULL, token.kind());
  }

  // "DEFAULT" は DEFAULT トークンになることを確認
  @Test
  void shouldTokenizeAsDefault_whenDefaultKeywordIsGiven() {

    ParsedCsv parsed =
        parsedOf(
            List.of(ColumnType.INT), List.of("id"), List.of(new CsvRow(4, List.of("DEFAULT"))));

    ValueTokenizer tokenizer = new ValueTokenizer();
    ValueTokenizationResult result = tokenizer.tokenize(parsed);

    assertTrue(result.isOk());
    ValueToken token = result.tokenized().orElseThrow().rows().get(0).tokenAt(0);
    assertEquals(ValueToken.Kind.DEFAULT, token.kind());
  }

  // text型で ""（ダブルクォート2文字）を指定すると EMPTY_STRING トークンになることを確認
  @Test
  void shouldTokenizeAsEmptyString_whenTextTypeAndEmptyStringTokenIsGiven() {

    ParsedCsv parsed =
        parsedOf(
            List.of(ColumnType.TEXT), List.of("name"), List.of(new CsvRow(4, List.of("\"\""))));

    ValueTokenizer tokenizer = new ValueTokenizer();
    ValueTokenizationResult result = tokenizer.tokenize(parsed);

    assertTrue(result.isOk());
    ValueToken token = result.tokenized().orElseThrow().rows().get(0).tokenAt(0);
    assertEquals(ValueToken.Kind.EMPTY_STRING, token.kind());
  }

  // text型以外で ""（ダブルクォート2文字）を指定するとエラーになることを確認
  @Test
  void shouldReturnErrors_whenEmptyStringTokenIsUsedForNonTextType() {

    ParsedCsv parsed =
        parsedOf(List.of(ColumnType.INT), List.of("id"), List.of(new CsvRow(4, List.of("\"\""))));

    ValueTokenizer tokenizer = new ValueTokenizer();
    ValueTokenizationResult result = tokenizer.tokenize(parsed);

    assertFalse(result.isOk());
    assertFalse(result.errors().isEmpty());

    // エラー内容のチェック（行番号/列名/型/入力値）
    assertEquals(4, result.errors().get(0).fileLine());
    assertEquals("id", result.errors().get(0).columnName());
    assertEquals("int", result.errors().get(0).type());
    assertEquals("\"\"", result.errors().get(0).input());
  }

  private static ParsedCsv parsedOf(
      List<ColumnType> types, List<String> headers, List<CsvRow> rows) {
    return new ParsedCsv("users", types, headers, rows);
  }
}
