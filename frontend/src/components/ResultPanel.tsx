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
    <div>
      <h3 className="text-lg font-bold text-slate-950">変換結果</h3>
      <p className="mt-2 text-sm leading-6 text-slate-600">
        変換を実行すると、ここに生成SQLまたはエラー一覧を表示する。
      </p>

      <div className="mt-6 rounded-xl border border-dashed border-slate-300 bg-slate-50 p-8 text-center text-sm text-slate-500">
        まだ変換結果はありません。
      </div>
    </div>
  );
}
