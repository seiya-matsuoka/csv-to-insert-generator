type AppHeaderProps = {
  apiBaseUrl: string;
};

export function AppHeader({ apiBaseUrl }: AppHeaderProps) {
  return (
    <header className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <div className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p className="text-sm font-medium text-slate-500">
            CSV to INSERT Generator
          </p>

          <h1 className="mt-2 text-3xl font-bold tracking-tight text-slate-950">
            CSVからINSERT SQLを生成するツール
          </h1>

          <p className="mt-3 max-w-3xl text-sm leading-6 text-slate-600">
            CSVフォーマットDをアップロードし、PostgreSQL向けのINSERT文を生成する。
            生成結果は画面表示、コピー、.sqlダウンロードに対応する。
          </p>
        </div>

        <div className="rounded-xl bg-slate-100 px-4 py-3 lg:min-w-[320px]">
          <p className="text-xs font-semibold text-slate-500">API</p>
          <p className="mt-1 break-all font-mono text-xs text-slate-700">
            {apiBaseUrl}
          </p>
        </div>
      </div>
    </header>
  );
}
