import { type ChangeEvent, type FormEvent, useMemo, useState } from "react";
import {
  convertCsv,
  getApiBaseUrl,
  type ConvertFailureResponse,
  type ConvertResponse,
  type ConvertSuccessResponse,
} from "./lib/api";

type FormMessage = {
  type: "info" | "error";
  text: string;
};

function App() {
  const apiBaseUrl = useMemo(() => getApiBaseUrl(), []);

  const [tableName, setTableName] = useState("users");
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [result, setResult] = useState<ConvertResponse | null>(null);
  const [message, setMessage] = useState<FormMessage | null>(null);
  const [isConverting, setIsConverting] = useState(false);

  const successResult = result?.ok ? result : null;
  const failureResult = result && !result.ok ? result : null;

  function handleFileChange(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0] ?? null;
    setSelectedFile(file);
    setResult(null);

    if (file) {
      setMessage({
        type: "info",
        text: `選択中のファイル: ${file.name}`,
      });
      return;
    }

    setMessage(null);
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const table = tableName.trim();

    if (!table) {
      setMessage({
        type: "error",
        text: "テーブル名を入力してください。",
      });
      return;
    }

    if (!selectedFile) {
      setMessage({
        type: "error",
        text: "CSVファイルを選択してください。",
      });
      return;
    }

    setIsConverting(true);
    setResult(null);
    setMessage({
      type: "info",
      text: "変換中です。",
    });

    try {
      const response = await convertCsv({
        table,
        file: selectedFile,
      });

      setResult(response);

      if (response.ok) {
        setMessage({
          type: "info",
          text: "SQLを生成しました。",
        });
      } else {
        setMessage({
          type: "error",
          text: "CSVの内容にエラーがあります。下のエラー一覧を確認してください。",
        });
      }
    } catch (error) {
      const text =
        error instanceof Error
          ? error.message
          : "変換中に予期しないエラーが発生しました。";

      setMessage({
        type: "error",
        text,
      });
    } finally {
      setIsConverting(false);
    }
  }

  return (
    <main className="min-h-screen bg-slate-50 px-4 py-8 text-slate-900">
      <div className="mx-auto max-w-6xl">
        <Header apiBaseUrl={apiBaseUrl} />

        <div className="mt-6 grid gap-6 lg:grid-cols-[420px_1fr]">
          <ConvertForm
            tableName={tableName}
            selectedFile={selectedFile}
            isConverting={isConverting}
            message={message}
            onTableNameChange={setTableName}
            onFileChange={handleFileChange}
            onSubmit={handleSubmit}
          />

          <ResultPanel
            successResult={successResult}
            failureResult={failureResult}
          />
        </div>
      </div>
    </main>
  );
}

type HeaderProps = {
  apiBaseUrl: string;
};

function Header({ apiBaseUrl }: HeaderProps) {
  return (
    <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <p className="text-sm font-medium text-slate-500">
        CSV to INSERT Generator
      </p>

      <h1 className="mt-2 text-3xl font-bold tracking-tight text-slate-950">
        CSVからINSERT SQLを生成するツール
      </h1>

      <p className="mt-4 max-w-3xl text-base leading-7 text-slate-600">
        CSVフォーマットDをアップロードし、PostgreSQL向けのINSERT文を生成する。
      </p>

      <div className="mt-6 rounded-xl bg-slate-100 p-4">
        <p className="text-sm font-semibold text-slate-700">接続先API</p>
        <p className="mt-1 break-all font-mono text-sm text-slate-700">
          {apiBaseUrl}
        </p>
      </div>
    </section>
  );
}

type ConvertFormProps = {
  tableName: string;
  selectedFile: File | null;
  isConverting: boolean;
  message: FormMessage | null;
  onTableNameChange: (value: string) => void;
  onFileChange: (event: ChangeEvent<HTMLInputElement>) => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
};

function ConvertForm({
  tableName,
  selectedFile,
  isConverting,
  message,
  onTableNameChange,
  onFileChange,
  onSubmit,
}: ConvertFormProps) {
  return (
    <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <div>
        <h2 className="text-xl font-bold text-slate-950">変換入力</h2>
        <p className="mt-2 text-sm leading-6 text-slate-600">
          backendの現在仕様に合わせて、テーブル名とCSVファイルを
          multipart/form-dataで送信する。
        </p>
      </div>

      <form className="mt-6 space-y-5" onSubmit={onSubmit}>
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
            className="mt-2 w-full rounded-xl border border-slate-300 bg-white px-4 py-3 text-sm outline-none transition focus:border-slate-500 focus:ring-4 focus:ring-slate-100"
          />
          <p className="mt-2 text-xs leading-5 text-slate-500">
            例: users / orders / book_logs。CSV内の #table
            行もbackend側でこの値に合わせる。
          </p>
        </div>

        <div>
          <label
            htmlFor="csvFile"
            className="block text-sm font-semibold text-slate-800"
          >
            CSVファイル
          </label>
          <input
            id="csvFile"
            type="file"
            accept=".csv,text/csv"
            onChange={onFileChange}
            className="mt-2 block w-full cursor-pointer rounded-xl border border-dashed border-slate-300 bg-slate-50 px-4 py-4 text-sm text-slate-700 file:mr-4 file:rounded-lg file:border-0 file:bg-slate-900 file:px-4 file:py-2 file:text-sm file:font-semibold file:text-white hover:bg-slate-100"
          />

          {selectedFile ? (
            <p className="mt-2 text-xs leading-5 text-slate-500">
              選択中: {selectedFile.name}（{formatBytes(selectedFile.size)}）
            </p>
          ) : (
            <p className="mt-2 text-xs leading-5 text-slate-500">
              CSVフォーマットのファイルを選択する。
            </p>
          )}
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

        <button
          type="submit"
          disabled={isConverting}
          className="w-full rounded-xl bg-slate-950 px-5 py-3 text-sm font-bold text-white shadow-sm transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:bg-slate-400"
        >
          {isConverting ? "変換中..." : "SQLを生成する"}
        </button>
      </form>
    </section>
  );
}

type ResultPanelProps = {
  successResult: ConvertSuccessResponse | null;
  failureResult: ConvertFailureResponse | null;
};

function ResultPanel({ successResult, failureResult }: ResultPanelProps) {
  if (successResult) {
    return (
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
        <div className="flex flex-col gap-2 md:flex-row md:items-start md:justify-between">
          <div>
            <h2 className="text-xl font-bold text-slate-950">生成SQL</h2>
            <p className="mt-2 text-sm leading-6 text-slate-600">
              次工程でコピーと.sqlダウンロード機能を追加する。
            </p>
          </div>

          <div className="rounded-xl bg-emerald-50 px-3 py-2 text-xs font-semibold text-emerald-700">
            生成成功
          </div>
        </div>

        <div className="mt-4 rounded-xl bg-slate-100 p-4">
          <p className="text-xs font-semibold text-slate-500">出力ファイル名</p>
          <p className="mt-1 break-all font-mono text-sm text-slate-700">
            {successResult.outputFileName}
          </p>
        </div>

        <textarea
          readOnly
          value={successResult.sql}
          className="mt-4 h-130 w-full resize-y rounded-xl border border-slate-200 bg-slate-950 p-4 font-mono text-sm leading-6 text-slate-100 outline-none"
        />
      </section>
    );
  }

  if (failureResult) {
    return (
      <section className="rounded-2xl border border-red-200 bg-white p-6 shadow-sm">
        <div>
          <h2 className="text-xl font-bold text-red-700">変換エラー</h2>
          <p className="mt-2 text-sm leading-6 text-slate-600">
            CSVの内容を修正して、再度変換する。
            {failureResult.truncated
              ? ` エラーは最大${failureResult.maxErrors}件で打ち切られている。`
              : ""}
          </p>
        </div>

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
                {failureResult.errors.map((error, index) => (
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
      </section>
    );
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

function formatBytes(size: number): string {
  if (size < 1024) {
    return `${size} B`;
  }

  if (size < 1024 * 1024) {
    return `${(size / 1024).toFixed(1)} KB`;
  }

  return `${(size / 1024 / 1024).toFixed(1)} MB`;
}

export default App;
