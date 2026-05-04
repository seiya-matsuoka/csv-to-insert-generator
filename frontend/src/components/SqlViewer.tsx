import { useState } from "react";
import type { ConvertSuccessResponse } from "../types/convert";

type SqlViewerProps = {
  result: ConvertSuccessResponse;
};

type CopyMessageState = {
  sql: string;
  text: string;
};

export function SqlViewer({ result }: SqlViewerProps) {
  const [copyMessage, setCopyMessage] = useState<CopyMessageState | null>(null);

  const visibleCopyMessage =
    copyMessage?.sql === result.sql ? copyMessage.text : null;

  async function handleCopy(sql: string) {
    try {
      await navigator.clipboard.writeText(sql);
      setCopyMessage({
        sql,
        text: "コピーしました。",
      });
    } catch {
      setCopyMessage({
        sql,
        text: "コピーに失敗しました。手動で選択してコピーしてください。",
      });
    }
  }

  function handleDownload(result: ConvertSuccessResponse) {
    const blob = new Blob([result.sql], {
      type: "text/sql;charset=utf-8",
    });

    const url = URL.createObjectURL(blob);
    const anchor = document.createElement("a");

    anchor.href = url;
    anchor.download = result.outputFileName;
    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();

    URL.revokeObjectURL(url);
  }

  return (
    <div>
      <div className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
        <div>
          <h3 className="text-lg font-bold text-slate-950">生成SQL</h3>
          <p className="mt-2 text-sm leading-6 text-slate-600">
            生成されたSQLは、そのままコピーまたは.sqlファイルとして保存できる。
          </p>
        </div>

        <div className="rounded-xl bg-emerald-50 px-3 py-2 text-xs font-semibold text-emerald-700">
          生成成功
        </div>
      </div>

      <div className="mt-4 rounded-xl bg-slate-100 p-4">
        <p className="text-xs font-semibold text-slate-500">出力ファイル名</p>
        <p className="mt-1 break-all font-mono text-sm text-slate-700">
          {result.outputFileName}
        </p>
      </div>

      <div className="mt-4 flex flex-col gap-3 sm:flex-row">
        <button
          type="button"
          onClick={() => void handleCopy(result.sql)}
          className="rounded-xl bg-slate-950 px-5 py-3 text-sm font-bold text-white shadow-sm transition hover:bg-slate-800"
        >
          SQLをコピー
        </button>

        <button
          type="button"
          onClick={() => handleDownload(result)}
          className="rounded-xl border border-slate-300 bg-white px-5 py-3 text-sm font-bold text-slate-800 shadow-sm transition hover:bg-slate-50"
        >
          .sqlをダウンロード
        </button>
      </div>

      {visibleCopyMessage ? (
        <p className="mt-3 text-sm text-slate-600">{visibleCopyMessage}</p>
      ) : null}

      <textarea
        readOnly
        value={result.sql}
        className="mt-4 h-140 w-full resize-y rounded-xl border border-slate-200 bg-slate-950 p-4 font-mono text-sm leading-6 text-slate-100 outline-none"
      />
    </div>
  );
}
