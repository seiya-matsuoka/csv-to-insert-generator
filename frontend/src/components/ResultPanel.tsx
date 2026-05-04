import type {
  ConvertFailureResponse,
  ConvertSuccessResponse,
} from "../types/convert";
import { ErrorTable } from "./ErrorTable";
import { SqlViewer } from "./SqlViewer";

type ResultPanelProps = {
  successResult: ConvertSuccessResponse | null;
  failureResult: ConvertFailureResponse | null;
};

export function ResultPanel({
  successResult,
  failureResult,
}: ResultPanelProps) {
  if (successResult) {
    return <SqlViewer result={successResult} />;
  }

  if (failureResult) {
    return <ErrorTable result={failureResult} />;
  }

  return (
    <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <h2 className="text-xl font-bold text-slate-950">変換結果</h2>
      <p className="mt-2 text-sm leading-6 text-slate-600">
        テーブル名とCSVファイルを指定して変換すると、ここに生成SQLまたはエラー一覧を表示する。
      </p>

      <div className="mt-6 rounded-xl border border-dashed border-slate-300 bg-slate-50 p-8 text-center text-sm text-slate-500">
        まだ変換結果はありません。
      </div>
    </section>
  );
}
