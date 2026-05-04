/**
 * /convert のエラー1件分。
 *
 * backend の ErrorDto に対応する。
 */
export type ConvertError = {
  fileLineNumber: number;
  columnName: string;
  type: string;
  inputValue: string;
  reason: string;
};

/**
 * /convert 成功レスポンス。
 */
export type ConvertSuccessResponse = {
  ok: true;
  generatedAt: string;
  outputFileName: string;
  sql: string;
};

/**
 * /convert 失敗レスポンス。
 */
export type ConvertFailureResponse = {
  ok: false;
  generatedAt: string;
  errors: ConvertError[];
  truncated: boolean;
  maxErrors: number;
};

/**
 * /convert のレスポンス全体。
 */
export type ConvertResponse = ConvertSuccessResponse | ConvertFailureResponse;

/**
 * /convert 呼び出し時の入力。
 */
export type ConvertCsvParams = {
  table: string;
  file: File;
};
