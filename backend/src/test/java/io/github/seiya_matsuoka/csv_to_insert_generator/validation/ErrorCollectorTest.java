package io.github.seiya_matsuoka.csv_to_insert_generator.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** {@link ErrorCollector} の基本挙動テスト。 */
public class ErrorCollectorTest {

  // エラー収集上限を超えて add された場合、保持される件数が上限で止まり、上限超過が発生したことが truncated=true で分かることを確認
  @Test
  void shouldSetTruncatedAndKeepMaxSize_whenErrorsExceedLimit() {
    ErrorCollector collector = new ErrorCollector(2);

    // 1件目・2件目は保持される
    collector.add(error(4, "id", "int", "x", "整数ではありません"));
    collector.add(error(5, "name", "text", "a", "OKのはず（ダミー）"));
    // 3件目は上限超過なので保持されず truncated が true になる
    collector.add(error(6, "is_active", "bool", "maybe", "true/falseではありません"));

    assertEquals(2, collector.size());
    assertTrue(collector.isTruncated());
    assertEquals(2, collector.errors().size());
  }

  // 収集件数が上限以内であれば truncated=false のままであり、エラーが追加されていれば hasErrors=true になることを確認
  @Test
  void shouldNotSetTruncated_whenErrorsWithinLimit() {
    ErrorCollector collector = new ErrorCollector(2);

    collector.add(error(4, "id", "int", "x", "整数ではありません"));
    collector.add(error(5, "is_active", "bool", "maybe", "true/falseではありません"));

    assertEquals(2, collector.size());
    assertFalse(collector.isTruncated());
    assertTrue(collector.hasErrors());
  }

  private static ValidationError error(
      int fileLine, String column, String type, String input, String reason) {
    return new ValidationError(fileLine, column, type, input, reason);
  }
}
