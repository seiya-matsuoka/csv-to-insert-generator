package io.github.seiya_matsuoka.csv_to_insert_generator.api.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Jackson {@link ObjectMapper} の生成・共有を行うFactory。
 *
 * <ul>
 *   <li>生成設定を一箇所に集約して見通しを良くする
 *   <li>将来的に出力フォーマット（日時など）を調整しやすくする
 * </ul>
 *
 * <p>ObjectMapperは設定後はスレッドセーフに使えるため、単一インスタンスを共有する。
 */
public final class ObjectMapperFactory {

  private static final ObjectMapper MAPPER = createInternal();

  private ObjectMapperFactory() {}

  /**
   * 共有ObjectMapperを返す。
   *
   * @return ObjectMapper
   */
  public static ObjectMapper create() {
    return MAPPER;
  }

  /**
   * ObjectMapperを構築する。
   *
   * @return 設定済みObjectMapper
   */
  private static ObjectMapper createInternal() {
    ObjectMapper mapper = new ObjectMapper();

    // Java Time（LocalDateTime等）対応
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // nullは出さない（レスポンスをスッキリさせる）
    mapper.setDefaultPropertyInclusion(
        JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_NULL));

    return mapper;
  }
}
