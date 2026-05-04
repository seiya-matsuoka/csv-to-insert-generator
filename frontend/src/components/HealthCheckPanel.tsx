import type { HealthCheckState } from "../types/ui";

type HealthCheckPanelProps = {
  healthCheck: HealthCheckState;
  onCheck: () => void;
};

export function HealthCheckPanel({
  healthCheck,
  onCheck,
}: HealthCheckPanelProps) {
  const badgeClassName =
    healthCheck.status === "ok"
      ? "bg-emerald-50 text-emerald-700 ring-emerald-200"
      : healthCheck.status === "error"
        ? "bg-red-50 text-red-700 ring-red-200"
        : "bg-amber-50 text-amber-700 ring-amber-200";

  const label =
    healthCheck.status === "ok"
      ? "接続OK"
      : healthCheck.status === "error"
        ? "接続失敗"
        : "確認中";

  return (
    <section className="h-full rounded-2xl border border-slate-200 bg-white px-5 py-4 shadow-sm">
      <div className="flex h-full flex-col justify-between gap-3">
        <div>
          <div className="flex flex-wrap items-center gap-2">
            <h2 className="text-sm font-bold text-slate-950">API</h2>
            <span
              className={`inline-flex items-center rounded-full px-3 py-1 text-xs font-bold ring-1 ${badgeClassName}`}
            >
              {label}
            </span>
          </div>

          <p className="mt-2 line-clamp-2 text-xs leading-5 text-slate-600">
            {healthCheck.message}
          </p>

          {healthCheck.checkedAt ? (
            <p className="mt-1 text-xs text-slate-500">
              最終確認: {healthCheck.checkedAt}
            </p>
          ) : null}
        </div>

        <button
          type="button"
          onClick={onCheck}
          disabled={healthCheck.status === "checking"}
          className="w-full rounded-xl border border-slate-300 bg-white px-4 py-2 text-xs font-bold text-slate-800 shadow-sm transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:bg-slate-100 disabled:text-slate-400"
        >
          {healthCheck.status === "checking" ? "確認中..." : "再チェック"}
        </button>
      </div>
    </section>
  );
}
