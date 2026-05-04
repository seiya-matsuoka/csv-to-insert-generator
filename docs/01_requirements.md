# 要件・仕様まとめ（csv-to-insert-generator）

## 1. 概要

`csv-to-insert-generator` は、CSV ファイルから PostgreSQL 向けの `INSERT` 文 SQL を生成する Web ツール。

ブラウザ画面からテーブル名と CSV ファイルを指定して変換を実行し、生成された SQL を画面上で確認・コピー・ダウンロードできる。

入力内容に不備がある場合は SQL を生成せず、エラー箇所と理由を一覧表示する。

---

## 2. 目的

主な目的は次の通り。

- テストデータ投入用の `INSERT` 文作成を効率化する
- CSV の値が想定する型に合っているか事前に確認する
- SQL 実行前に、行番号・列名・理由付きで入力ミスを把握できるようにする
- Java / React / Docker / デプロイを含む小規模な業務改善ツールの実装例とする

---

## 3. 想定利用シーン

- 開発中に、DB へ投入するテストデータを SQL 化したい
- CSV で作成したデータを、PostgreSQL の `INSERT` 文に変換したい
- 手作業で `INSERT` 文を書く手間を減らしたい
- データ投入前に、型不正や列数不一致を確認したい

---

## 4. 技術スタック

### 4.1 backend

- Java 21
- Maven
- Java 標準 `HttpServer`
- Apache Commons CSV
- Apache Commons FileUpload
- Jackson
- JUnit 5
- Docker
- Render

### 4.2 frontend

- React
- TypeScript
- Vite
- Tailwind CSS v4
- Vercel

### 4.3 デプロイ構成

frontend と backend は分離してデプロイする。

- frontend: Vercel
- backend: Render Web Service
- frontend から backend へ API 通信を行う
- backend 側で CORS を制御する

---

## 5. 主な機能

### 5.1 frontend 機能

- API ヘルスチェック
- テンプレート CSV ダウンロード
- サンプル CSV ダウンロード
- テーブル名入力
- CSV ファイル選択
- CSV 変換実行
- 生成 SQL 表示
- 生成 SQL コピー
- `.sql` ファイルダウンロード
- エラー一覧表示

### 5.2 backend 機能

- ヘルスチェック API
- テンプレート CSV 配信
- サンプル CSV 配信
- CSV アップロード受付
- CSV 解析
- 値の解釈
- 型検証
- `INSERT` SQL 生成
- 成功 / 失敗レスポンス返却

---

## 6. 入力仕様

### 6.1 入力方法

画面から次の2つを指定する。

- テーブル名
- CSV ファイル

変換実行時は、`multipart/form-data` として backend の `/convert` に送信する。

送信フィールドは次の通り。

| field   | 内容                 |
| ------- | -------------------- |
| `table` | 変換対象のテーブル名 |
| `file`  | CSV ファイル         |

---

## 7. テーブル名仕様

テーブル名は画面入力の値を使用する。

CSV 内に `#table=...` 行がある場合でも、backend 側で画面入力のテーブル名に合わせる。
CSV 内に `#table=...` 行がない場合は、backend 側で先頭に `#table=<table>` を補う。

使用できるテーブル名は次の形式。

```
[A-Za-z_][A-Za-z0-9_]*
```

例:

```
users
user_logs
order_items
```

次のような指定は初期スコープ外。

```
public.users
user-logs
"Users"
```

---

## 8. CSV フォーマット

CSV は、型情報とカラム情報を含む専用フォーマットを使用する。

基本例:

```
#table=users
#types=int,text,bool,date,timestamp,uuid
id,name,is_active,birthday,created_at,user_uuid
1,Alice,true,1990-01-02,2026-01-05 10:00:00,550e8400-e29b-41d4-a716-446655440000
```

行構造:

| 行        | 内容                         |
| --------- | ---------------------------- |
| 1行目     | `#table=<table>`             |
| 2行目     | `#types=<type1>,<type2>,...` |
| 3行目     | カラム名                     |
| 4行目以降 | データ行                     |

ただし、実際のテーブル名は画面入力値が優先される。

---

## 9. CSV 基本ルール

- 文字コードは UTF-8
- UTF-8 BOM は許容
- 改行は LF / CRLF を許容
- 区切り文字はカンマ
- ヘッダ行は必須
- `#types` 行は必須
- データ行は 0 行以上
- データ行の列数はヘッダ列数と一致必須
- 列名の重複はエラー
- Excel（`.xlsx`）入力は非対応

---

## 10. 対応する型

`#types` 行で指定できる型は次の通り。

- `text`
- `int`
- `decimal`
- `bool`
- `date`
- `timestamp`
- `uuid`

未知の型名はエラーとする。

---

## 11. 値の扱い

CSV のセル値は、次のルールで解釈する。

| 入力      | 解釈      |
| --------- | --------- |
| 空欄      | `NULL`    |
| `NULL`    | `NULL`    |
| `DEFAULT` | `DEFAULT` |
| `""`      | 空文字    |
| その他    | 通常値    |

補足:

- `NULL` / `DEFAULT` は大文字・クォートなしの場合に特別扱いする
- `DEFAULT` は INSERT 文の値として `DEFAULT` を出力する
- 空文字 `""` は `text` 型のみ許可する
- `text` 型以外で空文字を指定した場合はエラー

---

## 12. 型検証

通常値は、列ごとの型に合わせて検証する。

| 型          | 検証概要                              |
| ----------- | ------------------------------------- |
| `text`      | 任意の文字列を許可                    |
| `int`       | 整数として解釈できること              |
| `decimal`   | 通常の整数・小数として解釈できること  |
| `bool`      | `true` / `false` として解釈できること |
| `date`      | 日付として解釈できること              |
| `timestamp` | 日時として解釈できること              |
| `uuid`      | UUID として解釈できること             |

型検証でエラーが1件でもある場合、SQL は生成しない。

---

## 13. 出力 SQL 仕様

生成する SQL は PostgreSQL 向けとする。

出力方針:

- SQL ファイル先頭にコメントを付ける
- `BEGIN;` / `COMMIT;` で囲む
- 1データ行につき1つの `INSERT` 文を生成する
- multi-row INSERT は使用しない
- カラムリストを必ず指定する

出力例:

```
-- csv-to-insert-generator
-- table: users
-- input: sample_1.csv
-- rows: 2
-- generated_at: 2026-01-05 10:00:00

BEGIN;

INSERT INTO users (id, name) VALUES (1, 'Alice');
INSERT INTO users (id, name) VALUES (2, 'Bob');

COMMIT;
```

---

## 14. SQL ファイル名

変換成功時、ダウンロード用ファイル名を次の形式で返す。

```
insert_<table>_<timestamp>.sql
```

例:

```
insert_users_20260105_100000.sql
```

---

## 15. エラー仕様

### 15.1 エラー収集

- 可能な範囲で複数のエラーを収集する
- エラーが1件でもある場合、SQL は生成しない
- エラー収集上限は 100 件
- 上限を超えた場合は、打ち切り状態を画面に表示する

### 15.2 エラー表示項目

画面では、次の情報を一覧表示する。

- 行番号
- 列名
- 型
- 入力値
- 理由

行番号は、CSV ファイル先頭からの行番号を使用する。

---

## 16. API 概要

backend は次のエンドポイントを提供する。

| Method | Path            | 内容             |
| ------ | --------------- | ---------------- |
| `GET`  | `/healthz`      | 稼働確認         |
| `GET`  | `/template.csv` | テンプレート CSV |
| `GET`  | `/sample1.csv`  | サンプル CSV ①   |
| `GET`  | `/sample2.csv`  | サンプル CSV ②   |
| `POST` | `/convert`      | CSV 変換         |

### 16.1 `/convert`

リクエスト形式:

```
multipart/form-data
```

必須フィールド:

- `table`
- `file`

成功時は、生成 SQL と出力ファイル名を返す。
失敗時は、エラー一覧を返す。

---

## 17. 画面仕様

画面は、次の要素で構成する。

- ヘッダー
- CSV ダウンロード
- API ヘルスチェック
- 変換ワークスペース
  - 変換入力
  - 変換結果

### 17.1 ヘッダー

アプリ名と簡単な説明を表示する。

### 17.2 CSV ダウンロード

次の CSV をダウンロードできる。

- テンプレート CSV
- サンプル CSV ①
- サンプル CSV ②

### 17.3 API ヘルスチェック

- 初回表示時に自動で接続確認する
- 手動で再チェックできる
- 接続状態を表示する

### 17.4 変換入力

次の項目を入力する。

- テーブル名
- CSV ファイル

### 17.5 変換結果

成功時:

- SQL を表示
- SQL をコピーできる
- `.sql` ファイルとしてダウンロードできる

失敗時:

- エラー一覧を表示する

---

## 18. 非機能要件・制約

- 入力ファイルサイズ上限は 2MiB
- 入力ファイルは永続保存しない
- 処理はメモリ内で完結する
- backend は CORS 設定で許可 Origin を制御する
- Render Free のコールドスタートを考慮し、画面上で API 接続状態を表示する
