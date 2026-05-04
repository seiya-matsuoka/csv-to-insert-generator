export function AppHeader() {
  return (
    <header className="rounded-2xl border border-slate-200 bg-white px-6 py-7 shadow-sm">
      <h1 className="mt-2 text-4xl font-bold tracking-tight text-slate-950">
        CSV to INSERT Generator
      </h1>

      <p className="mt-4 max-w-3xl text-sm leading-6 text-slate-600">
        CSVファイルからPostgreSQL向けのINSERT文を生成する小さな変換ツール。
      </p>
    </header>
  );
}
