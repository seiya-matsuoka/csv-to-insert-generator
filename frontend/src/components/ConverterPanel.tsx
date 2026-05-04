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
        <h2 className="mt-1 text-2xl font-bold tracking-tight text-slate-950">
          変換ワークスペース
        </h2>
        <p className="mt-2 text-sm leading-6 text-slate-600">
          テーブル名とCSVファイルを指定してSQLを生成する。結果は下のエリアに表示する。
        </p>
      </div>

      <div className="border-b border-slate-200 px-6 py-6">
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

      <div className="px-6 py-6">
        <ResultPanel
          successResult={successResult}
          failureResult={failureResult}
        />
      </div>
    </section>
  );
}
