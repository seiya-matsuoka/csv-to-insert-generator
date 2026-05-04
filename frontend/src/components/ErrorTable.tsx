import type { ConvertFailureResponse } from "../types/convert";

type ErrorTableProps = {
  result: ConvertFailureResponse;
};

export function ErrorTable({ result }: ErrorTableProps) {
  return (
    <div>
      <h3 className="text-lg font-bold text-red-700">変換エラー</h3>
      <p className="mt-1 text-sm leading-6 text-slate-600">
        CSVの内容を修正して、再度変換する。
        {result.truncated
          ? ` エラーは最大${result.maxErrors}件で打ち切られている。`
          : ""}
      </p>

      <div className="mt-5 overflow-hidden rounded-xl border border-slate-200">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-slate-200 text-sm">
            <thead className="bg-slate-50">
              <tr>
                <th className="whitespace-nowrap px-4 py-3 text-left font-semibold text-slate-700">
                  行
                </th>
                <th className="whitespace-nowrap px-4 py-3 text-left font-semibold text-slate-700">
                  列
                </th>
                <th className="whitespace-nowrap px-4 py-3 text-left font-semibold text-slate-700">
                  型
                </th>
                <th className="whitespace-nowrap px-4 py-3 text-left font-semibold text-slate-700">
                  入力値
                </th>
                <th className="whitespace-nowrap px-4 py-3 text-left font-semibold text-slate-700">
                  理由
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 bg-white">
              {result.errors.map((error, index) => (
                <tr
                  key={`${error.fileLineNumber}-${error.columnName}-${index}`}
                >
                  <td className="whitespace-nowrap px-4 py-3 font-mono text-slate-700">
                    {error.fileLineNumber}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 font-mono text-slate-700">
                    {error.columnName}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 font-mono text-slate-700">
                    {error.type}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 font-mono text-slate-700">
                    {error.inputValue}
                  </td>
                  <td className="min-w-70 px-4 py-3 text-slate-700">
                    {error.reason}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
