# 設計（csv-to-insert-generator）

## 1. 設計概要

`csv-to-insert-generator` は、CSV ファイルを PostgreSQL 向けの `INSERT` 文 SQL に変換する Web アプリケーション。

本アプリは frontend と backend を分離して構成する。

- frontend: Vercel にデプロイする React アプリケーション
- backend: Render にデプロイする Java API サーバ
- frontend から backend へ HTTP API でアクセスする
- backend は Java 標準 `HttpServer` を使用し、Web フレームワークは使用しない

変換処理の中心は backend の core ロジックに置き、HTTP 入出力や UI 表示とは分離する。

---

## 2. 全体アーキテクチャ

全体構成は次の通り。

```
Browser
  |
  | 画面表示 / 操作
  v
Vercel frontend
  |
  | HTTP API
  v
Render backend
  |
  | CSV解析 / 値解釈 / 型検証 / SQL生成
  v
JSON response
```

### 2.1 frontend

frontend は次を担当する。

- 画面表示
- CSV ダウンロードリンクの表示
- API ヘルスチェック
- テーブル名入力
- CSV ファイル選択
- `/convert` への multipart リクエスト送信
- 変換結果の表示
- SQL コピー
- SQL ダウンロード

### 2.2 backend

backend は次を担当する。

- HTTP サーバ起動
- CORS 制御
- API ルーティング
- テンプレート / サンプル CSV 配信
- multipart/form-data の受信
- CSV 変換ユースケースの実行
- JSON レスポンス生成

---

## 3. ディレクトリ構成

### 3.1 ルート構成

```
.
├─ backend/
├─ frontend/
└─ .vscode/
```

### 3.2 backend

```
backend/
├─ Dockerfile
├─ pom.xml
└─ src/
   ├─ main/
   │  ├─ java/
   │  │  └─ io/github/seiya_matsuoka/csv_to_insert_generator/
   │  │     ├─ api/
   │  │     ├─ app/
   │  │     ├─ csv/
   │  │     ├─ domain/
   │  │     ├─ sql/
   │  │     ├─ tokenize/
   │  │     ├─ usecase/
   │  │     ├─ validation/
   │  │     └─ validator/
   │  └─ resources/
   │     ├─ samples/
   │     └─ templates/
   └─ test/
      └─ java/
```

### 3.3 frontend

```
frontend/
├─ package.json
├─ vite.config.ts
├─ eslint.config.js
├─ index.html
├─ public/
└─ src/
   ├─ components/
   ├─ lib/
   ├─ types/
   ├─ utils/
   ├─ App.tsx
   ├─ index.css
   └─ main.tsx
```

---

## 4. backend 設計

### 4.1 backend のパッケージ構成

backend の base package は次の通り。

```
io.github.seiya_matsuoka.csv_to_insert_generator
```

主なパッケージと責務は次の通り。

| パッケージ      | 主な責務                                                   |
| --------------- | ---------------------------------------------------------- |
| `app`           | アプリ起動、HTTPサーバ、ルーティング、CORS、HTTPレスポンス |
| `app.handler`   | 各APIエンドポイントのハンドラ                              |
| `app.multipart` | `HttpExchange` から multipart/form-data を扱うための処理   |
| `api.dto`       | APIレスポンスDTO                                           |
| `api.json`      | Jackson `ObjectMapper` の設定                              |
| `csv`           | CSVフォーマットDのパース                                   |
| `domain`        | 型や値トークンなどのドメインモデル                         |
| `tokenize`      | CSVセル値の解釈                                            |
| `validator`     | 型検証                                                     |
| `validation`    | バリデーションエラーとエラー収集                           |
| `sql`           | SQLリテラル変換とINSERT文生成                              |
| `usecase`       | CSV変換ユースケースの統合                                  |

### 4.2 起動設計

起動エントリポイントは `app.Main`。

起動時に次を行う。

1. `PORT` 環境変数を読み取る
2. `PORT` が未設定または不正な場合は `8080` を使用する
3. `ALLOWED_ORIGINS` 環境変数を読み取る
4. `CorsPolicy` を構築する
5. `Router` を構築する
6. `HttpServer` を起動する

サーバ生成は `ServerFactory` に分離する。

`ServerFactory` は指定ポートで `HttpServer` を作成し、`/` に `Router` を登録する。
リクエストごとの分岐は `Router` が担当する。

### 4.3 ルーティング設計

`Router` は `method + path` でハンドラを切り替える簡易ルータ。

登録しているエンドポイントは次の通り。

| Method | Path            | Handler              |
| ------ | --------------- | -------------------- |
| `GET`  | `/healthz`      | `HealthzHandler`     |
| `GET`  | `/template.csv` | `CsvDownloadHandler` |
| `GET`  | `/sample1.csv`  | `CsvDownloadHandler` |
| `GET`  | `/sample2.csv`  | `CsvDownloadHandler` |
| `POST` | `/convert`      | `ConvertHandler`     |

`Router` は次の共通処理も担当する。

- CORS ヘッダの付与
- `OPTIONS` リクエストへの `204` 応答
- 未登録パスへの `404`
- ハンドラでの想定外例外に対する `500`

### 4.4 CORS 設計

frontend と backend は別オリジンにデプロイされるため、backend 側で CORS を制御する。

CORS の設定は `CorsPolicy` が担当する。

- 許可 Origin は `ALLOWED_ORIGINS` 環境変数で指定する
- カンマ区切りで複数指定できる
- Origin は完全一致で判定する
- 未設定時はローカル開発用に `http://localhost:5173` を許可する

例:

```
ALLOWED_ORIGINS=http://localhost:5173,https://example.vercel.app
```

`OPTIONS` は `Router` で共通処理し、許可済み Origin に対して必要な CORS ヘッダを返す。

### 4.5 APIレスポンス設計

JSON 生成には Jackson を使用する。

`api.json.ObjectMapperFactory` に `ObjectMapper` の設定を集約する。  
各APIの成功・失敗レスポンスは `api.dto` 配下のDTOを通して返す。

主なDTOは次の通り。

| DTO                  | 用途                          |
| -------------------- | ----------------------------- |
| `ConvertResponseDto` | `/convert` レスポンスの共通型 |
| `ConvertSuccessDto`  | 変換成功レスポンス            |
| `ConvertFailureDto`  | 変換失敗レスポンス            |
| `ErrorDto`           | エラー1件分                   |

手書きの JSON 文字列生成は行わない。

### 4.6 multipart 設計

`POST /convert` は `multipart/form-data` を受け取る。

multipart 処理には Apache Commons FileUpload を使用する。  
ただし、Java 標準 `HttpServer` は Servlet API ではないため、`HttpExchange` を FileUpload が扱える形式へ変換する必要がある。

そのため、次の2クラスに分けている。

| クラス                      | 責務                                                           |
| --------------------------- | -------------------------------------------------------------- |
| `HttpExchangeUploadContext` | `HttpExchange` を FileUpload 用の `UploadContext` に適合させる |
| `MultipartCsvExtractor`     | multipart から `table` と `file` を取り出す                    |

`MultipartCsvExtractor` は次を行う。

- multipart/form-data であることの確認
- `table` フィールドの取得
- `file` フィールドの取得
- テーブル名の簡易検証
- アップロードサイズ上限の適用

CSVの内容検証はこの層では行わず、変換ユースケース側に任せる。

---

## 5. 変換処理設計

### 5.1 変換処理の入口

変換処理の入口は `usecase.ConvertUseCase`。

`ConvertUseCase` は HTTP や UI に依存せず、次の入力を受け取る。

- CSV本文
- 入力ファイル名

戻り値は `ConvertResult`。

- 成功時: SQL本文、出力ファイル名、生成日時
- 失敗時: エラー一覧、打ち切りフラグ

### 5.2 変換処理の流れ

変換処理は次の順序で行う。

```
CSV文字列
  |
  v
CsvFormatDParser
  |
  v
ValueTokenizer
  |
  v
TypeValidator
  |
  v
InsertSqlGenerator
  |
  v
ConvertResult
```

#### 5.2.1 CSVパース

`CsvFormatDParser` が担当する。

主な処理:

- UTF-8 BOM の除去
- `#table` 行の確認
- `#types` 行の確認
- 型名の解決
- ヘッダ行の確認
- 列名の形式チェック
- 列名重複チェック
- データ行の列数チェック

パース結果は `ParsedCsv` として表す。

#### 5.2.2 値の解釈

`ValueTokenizer` が担当する。

CSVセル値を `ValueToken` に変換する。

主なトークン種別:

- `NULL`
- `DEFAULT`
- `EMPTY_STRING`
- `RAW`

この段階では、値が型に合うかまでは判断しない。

#### 5.2.3 型検証

`TypeValidator` が担当する。

`TokenizedCsv` の各値を、列ごとの `ColumnType` に基づいて検証する。

検証エラーがある場合は、`ValidationError` を収集して失敗結果を返す。

#### 5.2.4 SQL生成

`InsertSqlGenerator` が担当する。

主な処理:

- ヘッダコメント生成
- `BEGIN;` / `COMMIT;` の付与
- 1行1INSERTの生成
- SQLリテラル変換

SQLリテラル変換の詳細は `SqlLiteral` が担当する。

### 5.3 エラー処理設計

エラーは `ValidationError` で表現する。

主な項目は次の通り。

| 項目         | 内容                     |
| ------------ | ------------------------ |
| `fileLine`   | ファイル先頭からの行番号 |
| `columnName` | 列名                     |
| `type`       | 型                       |
| `input`      | 入力値                   |
| `reason`     | 理由                     |

エラー収集には `ErrorCollector` を使用する。

- 最大100件まで収集する
- 上限に達した場合は `truncated=true`
- 各段階でエラーがある場合は、次段階へ進まず失敗結果を返す

### 5.4 変換処理とHTTP層の関係

`ConvertHandler` は HTTP 層の責務に限定する。

主な処理:

1. multipart から `table` と `file` を取得する
2. CSV bytes を UTF-8 文字列に変換する
3. `table` フィールドに合わせて `#table` 行を補正する
4. `ConvertRequest` を作成する
5. `ConvertUseCase` を呼ぶ
6. `ConvertResult` をDTOへ変換する
7. JSONとして返す

CSVの詳細な解析・検証・SQL生成は `ConvertUseCase` 以降に委譲する。

---

## 6. frontend 設計

### 6.1 frontend の構成

frontend は React + TypeScript + Vite + Tailwind CSS v4 で構成する。

主なディレクトリは次の通り。

```
frontend/src/
├─ components/
├─ lib/
├─ types/
├─ utils/
├─ App.tsx
├─ index.css
└─ main.tsx
```

### 6.2 `lib`

`lib/api.ts` に backend API 呼び出しを集約する。

主な関数:

| 関数             | 用途                   |
| ---------------- | ---------------------- |
| `getApiBaseUrl`  | APIベースURL取得       |
| `buildApiUrl`    | API URL組み立て        |
| `getDownloadUrl` | CSVダウンロードURL生成 |
| `healthz`        | ヘルスチェック         |
| `convertCsv`     | CSV変換API呼び出し     |

APIベースURLは `VITE_API_BASE_URL` 環境変数で指定する。
未指定時は `http://localhost:8080` を使用する。

### 6.3 `types`

APIレスポンスやUI状態の型を置く。

| ファイル           | 内容                               |
| ------------------ | ---------------------------------- |
| `types/convert.ts` | `/convert` のレスポンス型          |
| `types/ui.ts`      | 画面メッセージやヘルスチェック状態 |

### 6.4 `utils`

表示用の小さなユーティリティを置く。

| ファイル          | 内容                           |
| ----------------- | ------------------------------ |
| `utils/format.ts` | ファイルサイズ、日時の表示整形 |

### 6.5 コンポーネント構成

現在の主なコンポーネントは次の通り。

| コンポーネント     | 責務                              |
| ------------------ | --------------------------------- |
| `AppHeader`        | アプリ名と説明表示                |
| `CsvDownloadPanel` | テンプレート / サンプルCSVリンク  |
| `HealthCheckPanel` | API接続状態表示と再チェック       |
| `ConverterPanel`   | 変換入力と変換結果の大枠          |
| `ConvertForm`      | テーブル名入力、CSV選択、変換実行 |
| `ResultPanel`      | 成功 / 失敗 / 未実行の切り替え    |
| `SqlViewer`        | SQL表示、コピー、ダウンロード     |
| `ErrorTable`       | エラー一覧表示                    |

`App.tsx` は、状態管理とコンポーネントの組み立てを担当する。

### 6.6 状態管理

状態管理ライブラリは使用しない。
`useState` と `useEffect` を中心に管理する。

主な状態:

- テーブル名
- 選択中のCSVファイル
- 変換結果
- フォームメッセージ
- 変換中フラグ
- ヘルスチェック状態

### 6.7 画面レイアウト

画面は次の順で構成する。

```
Header
CSV Download / Health Check
Converter Workspace
  - Convert Input
  - Convert Result
```

設計方針:

- 補助機能は上部に配置する
- 主機能である変換処理は中央にまとめる
- 入力から結果確認までが上下に流れるようにする
- SQL表示エリアは広く確保する

---

## 7. API設計

backend が提供するAPIは次の通り。

| Method | Path            | 内容            |
| ------ | --------------- | --------------- |
| `GET`  | `/healthz`      | ヘルスチェック  |
| `GET`  | `/template.csv` | テンプレートCSV |
| `GET`  | `/sample1.csv`  | サンプルCSV①    |
| `GET`  | `/sample2.csv`  | サンプルCSV②    |
| `POST` | `/convert`      | CSV変換         |

### 7.1 `POST /convert`

`multipart/form-data` で受け取る。

必須フィールド:

- `table`
- `file`

成功時は `ConvertSuccessDto` を返す。
失敗時は `ConvertFailureDto` を返す。

HTTPステータスの基本方針:

| 状態           | ステータス |
| -------------- | ---------- |
| 変換成功       | `200`      |
| CSV検証エラー  | `422`      |
| リクエスト不正 | `400`      |
| 想定外エラー   | `500`      |

---

## 8. デプロイ設計

### 8.1 backend

backend は Render Web Service に Docker でデプロイする。

Dockerfile は backend 配下に置く。

```
backend/Dockerfile
```

Dockerfile は multi-stage build とする。

- build stage: Maven でビルドし、依存jarを収集する
- runtime stage: Eclipse Temurin 21 JRE で起動する

実行は fat jar ではなく、`classes` と `dependency/*` を classpath に指定する。

```
java -cp 'classes:dependency/*' io.github.seiya_matsuoka.csv_to_insert_generator.app.Main
```

### 8.2 frontend

frontend は Vercel に Vite アプリとしてデプロイする。

設定:

- Root Directory: `frontend`
- Build Command: `npm run build`
- Output Directory: `dist`
- 環境変数: `VITE_API_BASE_URL`

### 8.3 環境変数

backend:

| 変数              | 内容                               |
| ----------------- | ---------------------------------- |
| `PORT`            | Render が付与する待受ポート        |
| `ALLOWED_ORIGINS` | CORSで許可するOriginのカンマ区切り |

frontend:

| 変数                | 内容          |
| ------------------- | ------------- |
| `VITE_API_BASE_URL` | backend のURL |

---

## 9. テスト設計

### 9.1 backend

backend は JUnit 5 でテストする。

主なテスト対象:

- CSVパーサ
- 値トークナイザ
- 型検証
- SQL生成
- ConvertUseCase
- Router
- ConvertHandler
- SmokeTest

実行コマンド:

```
cd backend
mvn test
```

### 9.2 frontend

frontend はビルド確認を主な確認手段とする。

実行コマンド:

```
cd frontend
npm run build
```

---

## 10. 主な設計判断

### 10.1 Webフレームワークを使わない

backend は Spring Boot などを使わず、Java 標準 `HttpServer` で実装する。

目的:

- Java標準APIでHTTPサーバを構成する経験を得る
- フレームワークなしの責務分割を学ぶ
- 小規模ツールとして過剰な構成を避ける

### 10.2 core を HTTP から分離する

CSV解析・型検証・SQL生成は `ConvertUseCase` 以下に集約する。

これにより、HTTP層に依存せずテストしやすい構成にする。

### 10.3 JSON生成はJacksonに任せる

手書きJSON生成ではなく、Jacksonを使用する。

理由:

- エスケープ処理を自前で持たなくてよい
- DTOをそのままシリアライズできる
- APIレスポンスの見通しがよい

### 10.4 multipart処理はライブラリを使う

multipart/form-data の解析は Apache Commons FileUpload を使用する。

理由:

- multipart を自前実装すると複雑になりやすい
- 実務でもライブラリ利用が現実的
- `HttpServer` 用の薄いアダプタだけ自前で用意する方針にできる

---

## 11. 制約

本設計では次を前提とする。

- 入力ファイルは永続保存しない
- 処理はメモリ内で完結する
- 入力ファイルサイズ上限は 2MiB
- エラー収集上限は 100 件
- DB への直接接続は行わない
- 出力は SQL 文字列とダウンロード用ファイル名までとする
