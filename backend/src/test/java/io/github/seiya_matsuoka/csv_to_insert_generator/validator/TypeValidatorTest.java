package io.github.seiya_matsuoka.csv_to_insert_generator.validator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.seiya_matsuoka.csv_to_insert_generator.domain.ColumnType;
import io.github.seiya_matsuoka.csv_to_insert_generator.domain.ValueToken;
import io.github.seiya_matsuoka.csv_to_insert_generator.tokenize.TokenizedCsv;
import io.github.seiya_matsuoka.csv_to_insert_generator.tokenize.TokenizedRow;
import java.util.List;
import org.junit.jupiter.api.Test;

/** {@link TypeValidator} のテスト。 */
public class TypeValidatorTest {

  // RAW値が各型として妥当ならOKになることを確認
  @Test
  void shouldReturnOk_whenAllRawValuesAreValidForTypes() {

    TokenizedCsv csv =
        tokenizedOf(
            List.of(
                ColumnType.INT,
                ColumnType.DECIMAL,
                ColumnType.BOOL,
                ColumnType.DATE,
                ColumnType.TIMESTAMP,
                ColumnType.UUID,
                ColumnType.TEXT),
            List.of("id", "price", "is_active", "birthday", "created_at", "user_uuid", "note"),
            List.of(
                new TokenizedRow(
                    4,
                    List.of(
                        ValueToken.ofRaw("1"),
                        ValueToken.ofRaw("12.34"),
                        ValueToken.ofRaw("true"),
                        ValueToken.ofRaw("1990-01-02"),
                        ValueToken.ofRaw("2026-01-05 10:00:00"),
                        ValueToken.ofRaw("550e8400-e29b-41d4-a716-446655440000"),
                        ValueToken.ofRaw("hello")))));

    TypeValidator validator = new TypeValidator();
    TypeValidationResult result = validator.validate(csv);

    assertTrue(result.isOk());
    assertTrue(result.errors().isEmpty());
  }

  // int列に数値として解釈できないRAWが来たらエラーになることを確認
  @Test
  void shouldReturnErrors_whenIntIsInvalid() {

    TokenizedCsv csv =
        tokenizedOf(
            List.of(ColumnType.INT),
            List.of("id"),
            List.of(new TokenizedRow(4, List.of(ValueToken.ofRaw("abc")))));

    TypeValidator validator = new TypeValidator();
    TypeValidationResult result = validator.validate(csv);

    assertFalse(result.isOk());
    assertFalse(result.errors().isEmpty());
  }

  // decimal列に小数として解釈できないRAWが来たらエラーになることを確認
  @Test
  void shouldReturnErrors_whenDecimalIsInvalid() {

    TokenizedCsv csv =
        tokenizedOf(
            List.of(ColumnType.DECIMAL),
            List.of("price"),
            List.of(new TokenizedRow(4, List.of(ValueToken.ofRaw("12..3")))));

    TypeValidator validator = new TypeValidator();
    TypeValidationResult result = validator.validate(csv);

    assertFalse(result.isOk());
    assertFalse(result.errors().isEmpty());
  }

  // bool列に true/false 以外が来たらエラーになることを確認
  @Test
  void shouldReturnErrors_whenBoolIsInvalid() {

    TokenizedCsv csv =
        tokenizedOf(
            List.of(ColumnType.BOOL),
            List.of("is_active"),
            List.of(new TokenizedRow(4, List.of(ValueToken.ofRaw("yes")))));

    TypeValidator validator = new TypeValidator();
    TypeValidationResult result = validator.validate(csv);

    assertFalse(result.isOk());
    assertFalse(result.errors().isEmpty());
  }

  // date列に yyyy-MM-dd 以外が来たらエラーになることを確認
  @Test
  void shouldReturnErrors_whenDateIsInvalid() {

    TokenizedCsv csv =
        tokenizedOf(
            List.of(ColumnType.DATE),
            List.of("birthday"),
            List.of(new TokenizedRow(4, List.of(ValueToken.ofRaw("1990/01/02")))));

    TypeValidator validator = new TypeValidator();
    TypeValidationResult result = validator.validate(csv);

    assertFalse(result.isOk());
    assertFalse(result.errors().isEmpty());
  }

  // timestamp列に許可フォーマット以外が来たらエラーになることを確認
  @Test
  void shouldReturnErrors_whenTimestampIsInvalid() {

    TokenizedCsv csv =
        tokenizedOf(
            List.of(ColumnType.TIMESTAMP),
            List.of("created_at"),
            List.of(new TokenizedRow(4, List.of(ValueToken.ofRaw("2026-01-05")))));

    TypeValidator validator = new TypeValidator();
    TypeValidationResult result = validator.validate(csv);

    assertFalse(result.isOk());
    assertFalse(result.errors().isEmpty());
  }

  // uuid列に UUID として解釈できないRAWが来たらエラーになることを確認
  @Test
  void shouldReturnErrors_whenUuidIsInvalid() {

    TokenizedCsv csv =
        tokenizedOf(
            List.of(ColumnType.UUID),
            List.of("user_uuid"),
            List.of(new TokenizedRow(4, List.of(ValueToken.ofRaw("not-a-uuid")))));

    TypeValidator validator = new TypeValidator();
    TypeValidationResult result = validator.validate(csv);

    assertFalse(result.isOk());
    assertFalse(result.errors().isEmpty());
  }

  // NULL/DEFAULT は全型で許可され、エラーにならないことを確認
  @Test
  void shouldReturnOk_whenTokenIsNullOrDefault() {

    TokenizedCsv csv =
        tokenizedOf(
            List.of(ColumnType.INT, ColumnType.DATE),
            List.of("id", "birthday"),
            List.of(
                new TokenizedRow(
                    4, List.of(ValueToken.ofNull(""), ValueToken.ofDefault("DEFAULT")))));

    TypeValidator validator = new TypeValidator();
    TypeValidationResult result = validator.validate(csv);

    assertTrue(result.isOk());
    assertTrue(result.errors().isEmpty());
  }

  private static TokenizedCsv tokenizedOf(
      List<ColumnType> types, List<String> headers, List<TokenizedRow> rows) {
    return new TokenizedCsv("users", types, headers, rows);
  }
}
