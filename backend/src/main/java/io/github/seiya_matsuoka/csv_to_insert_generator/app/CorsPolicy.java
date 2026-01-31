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
 * <p>分離デプロイ（Vercel frontend → Render backend）では、ブラウザが 「別オリジンのAPI呼び出し」をブロックするため CORS 対応が必須となる。
 *
 * <p>ALLOWED_ORIGINS（カンマ区切り）に一致した Origin のみ許可する。
 *
 * <p>このクラスは「許可する Origin の判定」と「レスポンスヘッダ付与」を担当する。 実際の応答（204）などは Router 側で行う。
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
   * <p>env が未指定の場合、defaults を採用してローカル開発を行いやすくする。
   *
   * @param commaSeparated カンマ区切り文字列（null/空の場合は defaults を採用）
   * @param defaults デフォルト許可Origin（ローカル開発用など）
   * @return CorsPolicy
   */
  public static CorsPolicy fromCommaSeparated(String commaSeparated, List<String> defaults) {
    List<String> src = new ArrayList<>();

    // env が未指定なら defaults を使用
    if (commaSeparated == null || commaSeparated.isBlank()) {
      src.addAll(defaults == null ? List.of() : defaults);
    } else {
      // 指定されている場合はカンマで分割し、trimして空要素を除外
      String[] parts = commaSeparated.split(",");
      for (String p : parts) {
        String s = p == null ? "" : p.trim();
        if (!s.isEmpty()) {
          src.add(s);
        }
      }
    }

    // Set化して重複を排除（Originは完全一致で判定する）
    Set<String> set = new HashSet<>(src);
    return new CorsPolicy(set);
  }

  /**
   * Origin が許可されているか。
   *
   * <p>Origin は完全一致で判定する（部分一致やワイルドカードは使わない）。
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
   * <p>Access-Control-Allow-Origin は "*" ではなく、 許可した Origin をそのまま返す（分離デプロイの安全な運用）。
   *
   * <p>Origin が無い場合（curlや同一オリジンなど）は何もしない。
   *
   * @param requestHeaders リクエストヘッダ
   * @param responseHeaders レスポンスヘッダ
   */
  public void apply(Headers requestHeaders, Headers responseHeaders) {
    Objects.requireNonNull(requestHeaders, "requestHeaders is required");
    Objects.requireNonNull(responseHeaders, "responseHeaders is required");

    String origin = requestHeaders.getFirst("Origin");
    if (origin == null) {
      return; // same-origin / curl 等ではOriginが付かないことが多い
    }

    String trimmed = origin.trim();
    if (!isAllowed(trimmed)) {
      // 不許可なら CORS ヘッダは付けない（ブラウザ側でブロックされる）
      return;
    }

    // 許可Originのみ返す。Vary: Origin を付けてキャッシュ汚染を防ぐ。
    responseHeaders.set("Access-Control-Allow-Origin", trimmed);
    responseHeaders.set("Vary", "Origin");

    // 最小限の許可（今回のAPIで必要なものだけ）
    responseHeaders.set("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
    responseHeaders.set("Access-Control-Allow-Headers", "Content-Type");

    // プリフライトを短時間キャッシュして無駄なOPTIONSを減らす
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
