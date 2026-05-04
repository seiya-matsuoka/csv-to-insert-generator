const DEFAULT_API_BASE_URL = "http://localhost:8080";

export type ConvertError = {
  fileLineNumber: number;
  columnName: string;
  type: string;
  inputValue: string;
  reason: string;
};

export type ConvertSuccessResponse = {
  ok: true;
  generatedAt: string;
  outputFileName: string;
  sql: string;
};

export type ConvertFailureResponse = {
  ok: false;
  generatedAt: string;
  errors: ConvertError[];
  truncated: boolean;
  maxErrors: number;
};

export type ConvertResponse = ConvertSuccessResponse | ConvertFailureResponse;

export type ConvertCsvParams = {
  table: string;
  file: File;
};

/**
 * APIのベースURLを返す。
 *
 * Viteでは、ブラウザから参照する環境変数は VITE_ prefix が必要となる。
 * 末尾スラッシュはAPIパス結合時に邪魔になるため除去する。
 */
export function getApiBaseUrl(): string {
  const value = import.meta.env.VITE_API_BASE_URL ?? DEFAULT_API_BASE_URL;
  return String(value).replace(/\/+$/, "");
}

/**
 * APIのフルURLを組み立てる。
 *
 * @param path /healthz などのパス
 * @returns APIのフルURL
 */
export function buildApiUrl(path: string): string {
  const normalizedPath = path.startsWith("/") ? path : `/${path}`;
  return `${getApiBaseUrl()}${normalizedPath}`;
}

/**
 * 静的ファイルダウンロード用URLを返す。
 *
 * テンプレ/サンプルCSVダウンロードで使う。
 *
 * @param path /template.csv など
 * @returns ダウンロードURL
 */
export function getDownloadUrl(path: string): string {
  return buildApiUrl(path);
}

/**
 * backendのヘルスチェックを実行する。
 *
 * Render無料枠ではcold startで初回応答に時間がかかることがあるため、
 * 呼び出し側からAbortSignalを渡せるようにする。
 *
 * @param signal 中断用シグナル
 * @returns ok が返れば true
 */
export async function healthz(signal?: AbortSignal): Promise<boolean> {
  const response = await fetch(buildApiUrl("/healthz"), {
    method: "GET",
    signal,
  });

  if (!response.ok) {
    return false;
  }

  const text = await response.text();
  return text.trim() === "ok";
}

/**
 * CSVをINSERT SQLへ変換する。
 *
 * backendの現在仕様に合わせて、multipart/form-data で table と file を送る。
 * FormData利用時はブラウザがboundary付きContent-Typeを自動付与するため、
 * fetch側ではContent-Typeを明示しない。
 *
 * @param params テーブル名とCSVファイル
 * @returns 変換結果
 */
export async function convertCsv(
  params: ConvertCsvParams,
): Promise<ConvertResponse> {
  const table = params.table.trim();

  if (!table) {
    throw new Error("テーブル名を入力してください。");
  }

  const formData = new FormData();
  formData.append("table", table);
  formData.append("file", params.file);

  const response = await fetch(buildApiUrl("/convert"), {
    method: "POST",
    body: formData,
  });

  const contentType = response.headers.get("Content-Type") ?? "";
  if (!contentType.includes("application/json")) {
    const text = await response.text();
    throw new Error(
      `APIからJSON以外のレスポンスが返りました。status=${response.status}, body=${text}`,
    );
  }

  const json: unknown = await response.json();

  if (!isConvertResponse(json)) {
    throw new Error("APIレスポンスの形式が想定と異なります。");
  }

  return json;
}

function isConvertResponse(value: unknown): value is ConvertResponse {
  if (!isObject(value)) {
    return false;
  }

  if (value.ok === true) {
    return (
      typeof value.generatedAt === "string" &&
      typeof value.outputFileName === "string" &&
      typeof value.sql === "string"
    );
  }

  if (value.ok === false) {
    return (
      typeof value.generatedAt === "string" &&
      Array.isArray(value.errors) &&
      typeof value.truncated === "boolean" &&
      typeof value.maxErrors === "number"
    );
  }

  return false;
}

function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}
