import {
  type ChangeEvent,
  type FormEvent,
  useCallback,
  useEffect,
  useState,
} from "react";
import { AppHeader } from "./components/AppHeader";
import { ConverterPanel } from "./components/ConverterPanel";
import { CsvDownloadPanel } from "./components/CsvDownloadPanel";
import { HealthCheckPanel } from "./components/HealthCheckPanel";
import { convertCsv, healthz } from "./lib/api";
import type { ConvertResponse } from "./types/convert";
import type { FormMessage, HealthCheckState } from "./types/ui";
import { formatDateTime } from "./utils/format";

function App() {
  const [tableName, setTableName] = useState("users");
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [result, setResult] = useState<ConvertResponse | null>(null);
  const [message, setMessage] = useState<FormMessage | null>(null);
  const [isConverting, setIsConverting] = useState(false);
  const [healthCheck, setHealthCheck] = useState<HealthCheckState>({
    status: "checking",
    message: "APIサーバーとの接続を確認中です。",
    checkedAt: null,
  });

  const successResult = result?.ok ? result : null;
  const failureResult = result && !result.ok ? result : null;

  const runHealthCheck = useCallback(async () => {
    setHealthCheck({
      status: "checking",
      message:
        "APIサーバーとの接続を確認中です。Render無料枠では初回起動に時間がかかる場合があります。",
      checkedAt: null,
    });

    const controller = new AbortController();
    const timeoutId = window.setTimeout(() => {
      controller.abort();
    }, 45_000);

    try {
      const ok = await healthz(controller.signal);

      window.clearTimeout(timeoutId);

      if (ok) {
        setHealthCheck({
          status: "ok",
          message: "APIサーバーに接続できています。",
          checkedAt: formatDateTime(new Date()),
        });
        return;
      }

      setHealthCheck({
        status: "error",
        message:
          "APIサーバーから想定外の応答が返りました。時間を置いて再確認してください。",
        checkedAt: formatDateTime(new Date()),
      });
    } catch (error) {
      window.clearTimeout(timeoutId);

      const isAbort =
        error instanceof DOMException && error.name === "AbortError";

      setHealthCheck({
        status: "error",
        message: isAbort
          ? "APIサーバーの応答がタイムアウトしました。Renderの起動待ちの可能性があります。少し待ってから再確認してください。"
          : "APIサーバーに接続できませんでした。API URLまたはCORS設定を確認してください。",
        checkedAt: formatDateTime(new Date()),
      });
    }
  }, []);

  useEffect(() => {
    let ignore = false;

    const controller = new AbortController();
    const timeoutId = window.setTimeout(() => {
      controller.abort();
    }, 45_000);

    async function runInitialHealthCheck() {
      try {
        const ok = await healthz(controller.signal);

        if (ignore) {
          return;
        }

        if (ok) {
          setHealthCheck({
            status: "ok",
            message: "APIサーバーに接続できています。",
            checkedAt: formatDateTime(new Date()),
          });
          return;
        }

        setHealthCheck({
          status: "error",
          message:
            "APIサーバーから想定外の応答が返りました。時間を置いて再確認してください。",
          checkedAt: formatDateTime(new Date()),
        });
      } catch (error) {
        if (ignore) {
          return;
        }

        const isAbort =
          error instanceof DOMException && error.name === "AbortError";

        setHealthCheck({
          status: "error",
          message: isAbort
            ? "APIサーバーの応答がタイムアウトしました。Renderの起動待ちの可能性があります。少し待ってから再確認してください。"
            : "APIサーバーに接続できませんでした。API URLまたはCORS設定を確認してください。",
          checkedAt: formatDateTime(new Date()),
        });
      } finally {
        window.clearTimeout(timeoutId);
      }
    }

    void runInitialHealthCheck();

    return () => {
      ignore = true;
      window.clearTimeout(timeoutId);
      controller.abort();
    };
  }, []);

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
    <main className="min-h-screen bg-slate-50 px-4 py-6 text-slate-900 sm:py-8">
      <div className="mx-auto max-w-6xl">
        <AppHeader />

        <div className="mt-4 flex flex-col gap-4 lg:flex-row">
          <div className="lg:flex-7">
            <CsvDownloadPanel />
          </div>

          <div className="lg:flex-3">
            <HealthCheckPanel
              healthCheck={healthCheck}
              onCheck={() => void runHealthCheck()}
            />
          </div>
        </div>

        <div className="mt-6">
          <ConverterPanel
            tableName={tableName}
            selectedFile={selectedFile}
            isConverting={isConverting}
            message={message}
            successResult={successResult}
            failureResult={failureResult}
            onTableNameChange={setTableName}
            onFileChange={handleFileChange}
            onSubmit={handleSubmit}
          />
        </div>
      </div>
    </main>
  );
}

export default App;
