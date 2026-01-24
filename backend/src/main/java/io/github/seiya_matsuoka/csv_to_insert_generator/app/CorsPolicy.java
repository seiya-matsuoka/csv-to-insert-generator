package io.github.seiya_matsuoka.csv_to_insert_generator.app;

import com.sun.net.httpserver.Headers;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * CORS（Cross-Origin Resource Sharing）ポリシー。
 *
 * <p>分離デプロイ（Vercel frontend → Render backend）のため。 ALLOWED_ORIGINS（カンマ区切り）に一致した Origin のみ許可する。
 */
public final class CorsPolicy {

  private final Set<String> allowedOrigins;

  private CorsPolicy(Set<String> allowedOrigins) {
    this.allowedOrigins = allowedOrigins;
  }

  /**
   * カンマ区切りのOriginリストから CORS ポリシーを生成する。
   *
   * <p>例: "http://localhost:5173,https://xxx.vercel.app"
   *
   * @param commaSeparated カンマ区切り文字列（null/空の場合は defaults を採用）
   * @param defaults デフォルト許可Origin（ローカル開発用など）
   * @return CorsPolicy
   */
  public static CorsPolicy fromCommaSeparated(String commaSeparated, List<String> defaults) {
    List<String> src = new ArrayList<>();

    if (commaSeparated == null || commaSeparated.isBlank()) {
      src.addAll(defaults == null ? List.of() : defaults);
    } else {
      String[] parts = commaSeparated.split(",");
      for (String p : parts) {
        String s = p == null ? "" : p.trim();
        if (!s.isEmpty()) {
          src.add(s);
        }
      }
    }

    Set<String> set = new HashSet<>(src);
    return new CorsPolicy(set);
  }

  /**
   * Origin が許可されているか。
   *
   * @param origin Originヘッダ値
   * @return 許可ならtrue
   */
  public boolean isAllowed(String origin) {
    if (origin == null || origin.isBlank()) {
      return false;
    }
    return allowedOrigins.contains(origin.trim());
  }

  /**
   * CORSレスポンスヘッダを付与する（許可Originのみ）。
   *
   * @param requestHeaders リクエストヘッダ
   * @param responseHeaders レスポンスヘッダ
   */
  public void apply(Headers requestHeaders, Headers responseHeaders) {
    Objects.requireNonNull(requestHeaders, "requestHeaders is required");
    Objects.requireNonNull(responseHeaders, "responseHeaders is required");

    String origin = requestHeaders.getFirst("Origin");
    if (origin == null) {
      return;
    }

    String trimmed = origin.trim();
    if (!isAllowed(trimmed)) {
      return;
    }

    // Originごとに許可を返す（*ではなく）
    responseHeaders.set("Access-Control-Allow-Origin", trimmed);
    responseHeaders.set("Vary", "Origin");
    responseHeaders.set("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
    responseHeaders.set("Access-Control-Allow-Headers", "Content-Type");
    responseHeaders.set("Access-Control-Max-Age", "600");
  }

  /**
   * リクエストメソッドが OPTIONS かどうか。
   *
   * @param method HTTPメソッド
   * @return OPTIONSならtrue
   */
  public boolean isOptions(String method) {
    if (method == null) {
      return false;
    }
    return "OPTIONS".equals(method.trim().toUpperCase(Locale.ROOT));
  }
}
