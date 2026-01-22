package io.github.seiya_matsuoka.csv_to_insert_generator.sql;

import io.github.seiya_matsuoka.csv_to_insert_generator.domain.ColumnType;
import io.github.seiya_matsuoka.csv_to_insert_generator.domain.ValueToken;
import io.github.seiya_matsuoka.csv_to_insert_generator.tokenize.TokenizedCsv;
import io.github.seiya_matsuoka.csv_to_insert_generator.tokenize.TokenizedRow;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * TokenizedCsv から INSERT SQL（ヘッダコメント + BEGIN/COMMIT）を生成する。
 *
 * <p>ヘッダコメントとBEGIN/COMMITを付ける。
 *
 * <p>このクラスはSQL生成専用。
 */
public final class InsertSqlGenerator {

  private static final DateTimeFormatter HEADER_TS =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private final SqlLiteral sqlLiteral;

  /** デフォルトコンストラクタ。 */
  public InsertSqlGenerator() {
    this(new SqlLiteral());
  }

  /**
   * テスト容易性のために依存注入可能にする。
   *
   * @param sqlLiteral SQLリテラル生成器
   * @throws NullPointerException sqlLiteral が null の場合
   */
  public InsertSqlGenerator(SqlLiteral sqlLiteral) {
    this.sqlLiteral = Objects.requireNonNull(sqlLiteral, "sqlLiteral is required");
  }

  /**
   * SQL全文を生成する。
   *
   * @param tokenized トークン化済みCSV（型検証済み想定）
   * @param inputFileName 入力ファイル名（UIから渡る想定。ヘッダコメントに出す）
   * @param generatedAt 生成日時（ヘッダコメントに出す）
   * @return SQL全文（末尾改行付き）
   * @throws NullPointerException 引数がnullの場合
   * @throws IllegalArgumentException types数とheaders数が一致しない場合（想定外）
   */
  public String generate(TokenizedCsv tokenized, String inputFileName, LocalDateTime generatedAt) {
    Objects.requireNonNull(tokenized, "tokenized is required");
    Objects.requireNonNull(inputFileName, "inputFileName is required");
    Objects.requireNonNull(generatedAt, "generatedAt is required");

    // CSVパース工程で types数とheader数は一致している想定だが、
    // 想定外の入力が流入した場合、バグの原因になるためチェックする。
    if (tokenized.types().size() != tokenized.headers().size()) {
      throw new IllegalArgumentException(
          "#typesの列数とヘッダの列数が一致していません: types="
              + tokenized.types().size()
              + ", headers="
              + tokenized.headers().size());
    }

    // 生成するSQLは StringBuilderでまとめて構築する。（初期容量は適当な目安）。
    StringBuilder sb = new StringBuilder(4096);

    // ヘッダコメント（生成日時・対象テーブル・入力ファイル名・行数など）
    appendHeaderComment(
        sb, tokenized.tableName(), inputFileName, tokenized.rows().size(), generatedAt);

    // トランザクション開始
    sb.append("BEGIN;").append('\n').append('\n');

    // INSERTの前半（INSERT INTO ... (cols...)）は全行で共通なので、一度作って使い回す。
    // これにより、各行では VALUES(...) の部分だけ組み立てればよい。
    String insertPrefix = buildInsertPrefix(tokenized.tableName(), tokenized.headers());

    // typesは行ごとに同じなので、ループ外に出して参照を固定しておく。
    List<ColumnType> types = tokenized.types();

    // INSERT群を生成（データ行の数だけINSERT文を作る（1行 = 1INSERT））
    for (TokenizedRow row : tokenized.rows()) {
      sb.append(insertPrefix);

      // VALUES(...) の中をカンマ区切りで作る（StringJoinerで、先頭/末尾に余計な区切り文字が付かないよう安全に連結する）
      sb.append("VALUES (");
      StringJoiner values = new StringJoiner(", ");

      // 列の順序はヘッダ順固定。同じインデックスで type と token を取り、SQL表現へ変換して追加していく。
      for (int i = 0; i < types.size(); i++) {
        ColumnType type = types.get(i);
        ValueToken token = row.tokenAt(i);

        // 値トークン + 型 -> SQL表現（NULL/DEFAULT/'text'/123/TRUE など）
        values.add(sqlLiteral.toSql(type, token));
      }

      // VALUES節を確定し、文末にセミコロンを付けて1文完成。
      sb.append(values).append(");").append('\n');
    }

    // INSERT群とCOMMITの間を1行空ける。
    sb.append('\n');

    // トランザクション終了
    sb.append("COMMIT;").append('\n');

    // 末尾を改行付きにして、ファイルとして扱いやすくする。
    return sb.toString();
  }

  /**
   * ヘッダコメント（SQLファイル先頭の "-- ..." 形式）を付与する。
   *
   * @param sb 出力先
   * @param tableName 対象テーブル名
   * @param inputFileName 入力ファイル名（UIから渡される想定）
   * @param dataRowCount データ行数
   * @param generatedAt 生成日時
   */
  private void appendHeaderComment(
      StringBuilder sb,
      String tableName,
      String inputFileName,
      int dataRowCount,
      LocalDateTime generatedAt) {
    sb.append("-- csv-to-insert-generator").append('\n');
    sb.append("-- table: ").append(tableName).append('\n');
    sb.append("-- input: ").append(inputFileName).append('\n');
    sb.append("-- rows: ").append(dataRowCount).append('\n');
    sb.append("-- generated_at: ").append(generatedAt.format(HEADER_TS)).append('\n');
    sb.append('\n');
  }

  /**
   * INSERT文の共通部分（"INSERT INTO <table> (<col1>, <col2>, ...) "）を組み立てる。
   *
   * <p>カラム順はヘッダ順に従う。
   *
   * @param tableName テーブル名
   * @param headers カラム名一覧（ヘッダ）
   * @return INSERT文の前半（末尾にスペースを含む）
   */
  private String buildInsertPrefix(String tableName, List<String> headers) {
    // INSERT INTO table (col1, col2, ...) の形にする（カラムはヘッダ順固定）
    StringJoiner cols = new StringJoiner(", ");
    for (String h : headers) {
      cols.add(h);
    }
    return "INSERT INTO " + tableName + " (" + cols + ") ";
  }
}
