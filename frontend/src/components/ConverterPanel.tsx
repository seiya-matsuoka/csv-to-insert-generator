import type { ChangeEvent, FormEvent } from "react";
import type {
  ConvertFailureResponse,
  ConvertSuccessResponse,
} from "../types/convert";
import type { FormMessage } from "../types/ui";
import { ConvertForm } from "./ConvertForm";
import { ResultPanel } from "./ResultPanel";

type ConverterPanelProps = {
  tableName: string;
  selectedFile: File | null;
  isConverting: boolean;
  message: FormMessage | null;
  successResult: ConvertSuccessResponse | null;
  failureResult: ConvertFailureResponse | null;
  onTableNameChange: (value: string) => void;
  onFileChange: (event: ChangeEvent<HTMLInputElement>) => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
};

export function ConverterPanel({
  tableName,
  selectedFile,
  isConverting,
  message,
  successResult,
  failureResult,
  onTableNameChange,
  onFileChange,
  onSubmit,
}: ConverterPanelProps) {
  return (
    <section className="rounded-2xl border border-slate-200 bg-white shadow-sm">
      <div className="border-b border-slate-200 px-6 py-5">
        <p className="text-sm font-medium text-slate-500">Main Workflow</p>
        <h2 className="mt-1 text-2xl font-bold tracking-tight text-slate-950">
          CSVをINSERT SQLへ変換
        </h2>
        <p className="mt-2 text-sm leading-6 text-slate-600">
          左側でテーブル名とCSVファイルを指定し、右側で生成結果を確認する。
          成功時はSQLのコピーと.sqlダウンロードができる。
        </p>
      </div>

      <div className="grid gap-0 lg:grid-cols-[360px_1fr]">
        <div className="border-b border-slate-200 p-6 lg:border-b-0 lg:border-r">
          <ConvertForm
            tableName={tableName}
            selectedFile={selectedFile}
            isConverting={isConverting}
            message={message}
            onTableNameChange={onTableNameChange}
            onFileChange={onFileChange}
            onSubmit={onSubmit}
          />
        </div>

        <div className="min-w-0 p-6">
          <ResultPanel
            successResult={successResult}
            failureResult={failureResult}
          />
        </div>
      </div>
    </section>
  );
}
