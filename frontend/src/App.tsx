function App() {
  return (
    <main className="min-h-screen bg-slate-50 px-4 py-8 text-slate-900">
      <div className="mx-auto max-w-5xl">
        <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <p className="text-sm font-medium text-slate-500">
            CSV to INSERT Generator
          </p>

          <h1 className="mt-2 text-3xl font-bold tracking-tight text-slate-950">
            CSVからINSERT SQLを生成するツール
          </h1>

          <p className="mt-4 max-w-3xl text-base leading-7 text-slate-600">
            CSVフォーマットを読み込み、PostgreSQL向けのINSERT文を生成する。
          </p>
        </section>

        <section className="mt-6 grid gap-4 md:grid-cols-3">
          <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <h2 className="text-lg font-semibold text-slate-950">
              1. CSVを用意
            </h2>
            <p className="mt-2 text-sm leading-6 text-slate-600">
              次工程でテンプレートCSVとサンプルCSVのダウンロード機能を追加する。
            </p>
          </div>

          <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <h2 className="text-lg font-semibold text-slate-950">
              2. SQLへ変換
            </h2>
            <p className="mt-2 text-sm leading-6 text-slate-600">
              ファイル選択と変換ボタンを追加し、POST /convert を呼び出す。
            </p>
          </div>

          <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <h2 className="text-lg font-semibold text-slate-950">
              3. コピー・DL
            </h2>
            <p className="mt-2 text-sm leading-6 text-slate-600">
              生成されたSQLを画面表示し、コピーと.sqlダウンロードを可能にする。
            </p>
          </div>
        </section>
      </div>
    </main>
  );
}

export default App;
