import type { ChangeEvent, FormEvent } from "react";
import type { FormMessage } from "../types/ui";
import { formatBytes } from "../utils/format";

type ConvertFormProps = {
  tableName: string;
  selectedFile: File | null;
  isConverting: boolean;
  message: FormMessage | null;
  onTableNameChange: (value: string) => void;
  onFileChange: (event: ChangeEvent<HTMLInputElement>) => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
};

export function ConvertForm({
  tableName,
  selectedFile,
  isConverting,
  message,
  onTableNameChange,
  onFileChange,
  onSubmit,
}: ConvertFormProps) {
  return (
    <div>
      <div>
        <h3 className="text-lg font-bold text-slate-950">変換入力</h3>
        <p className="mt-1 text-sm leading-6 text-slate-600">
          入力内容を指定して変換を実行する。
        </p>
      </div>

      <form className="mt-5 space-y-4" onSubmit={onSubmit}>
        <div className="grid gap-4 lg:grid-cols-[240px_minmax(0,1fr)_200px]">
          <div>
            <label
              htmlFor="tableName"
              className="block text-sm font-semibold text-slate-800"
            >
              テーブル名
            </label>

            <input
              id="tableName"
              type="text"
              value={tableName}
              onChange={(event) => onTableNameChange(event.target.value)}
              placeholder="users"
              className="mt-2 h-12 w-full rounded-xl border border-slate-300 bg-white px-4 text-sm outline-none transition focus:border-slate-500 focus:ring-4 focus:ring-slate-100"
            />

            <p className="mt-2 text-xs leading-5 text-slate-500">例: users</p>
          </div>

          <div>
            <label
              htmlFor="csvFile"
              className="block text-sm font-semibold text-slate-800"
            >
              CSVファイル
            </label>

            <div className="mt-2 flex h-12 items-center gap-3 rounded-xl border border-dashed border-slate-300 bg-slate-50 px-3">
              <input
                id="csvFile"
                type="file"
                accept=".csv,text/csv"
                onChange={onFileChange}
                className="sr-only"
              />

              <label
                htmlFor="csvFile"
                className="shrink-0 cursor-pointer rounded-lg bg-slate-950 px-4 py-2 text-sm font-semibold text-white transition hover:bg-slate-800"
              >
                ファイルを選択
              </label>

              <span className="min-w-0 truncate text-sm text-slate-700">
                {selectedFile ? selectedFile.name : "選択されていません"}
              </span>
            </div>

            <p className="mt-2 text-xs leading-5 text-slate-500">
              {selectedFile
                ? `選択中: ${selectedFile.name}（${formatBytes(selectedFile.size)}）`
                : "CSVフォーマットDのファイルを選択する。"}
            </p>
          </div>

          <div className="flex flex-col lg:pt-7">
            <button
              type="submit"
              disabled={isConverting}
              className="h-12 rounded-xl bg-slate-950 px-5 text-sm font-bold text-white shadow-sm transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:bg-slate-400"
            >
              {isConverting ? "変換中..." : "SQLを生成する"}
            </button>

            <p className="mt-2 hidden text-xs leading-5 text-transparent lg:block">
              spacer
            </p>
          </div>
        </div>

        {message ? (
          <div
            className={
              message.type === "error"
                ? "rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm leading-6 text-red-700"
                : "rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm leading-6 text-slate-700"
            }
          >
            {message.text}
          </div>
        ) : null}
      </form>
    </div>
  );
}
