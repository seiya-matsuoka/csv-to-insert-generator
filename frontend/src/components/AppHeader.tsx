type AppHeaderProps = {
  apiBaseUrl: string;
};

export function AppHeader({ apiBaseUrl }: AppHeaderProps) {
  return (
    <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <p className="text-sm font-medium text-slate-500">
        CSV to INSERT Generator
      </p>

      <h1 className="mt-2 text-3xl font-bold tracking-tight text-slate-950">
        CSVからINSERT SQLを生成するツール
      </h1>

      <p className="mt-4 max-w-3xl text-base leading-7 text-slate-600">
        CSVフォーマットをアップロードし、PostgreSQL向けのINSERT文を生成する。
        生成結果は画面表示、コピー、.sqlダウンロードに対応する。
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
