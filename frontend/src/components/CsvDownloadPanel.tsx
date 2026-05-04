import { getDownloadUrl } from "../lib/api";

export function CsvDownloadPanel() {
  const links = [
    {
      label: "テンプレートCSV",
      description: "入力フォーマットだけを確認するためのCSV。",
      href: getDownloadUrl("/template.csv"),
      fileName: "template.csv",
    },
    {
      label: "サンプルCSV①",
      description: "基本的な値のみを含む小さめのサンプル。",
      href: getDownloadUrl("/sample1.csv"),
      fileName: "sample_1.csv",
    },
    {
      label: "サンプルCSV②",
      description: "NULL / DEFAULT / 空文字を含む確認用サンプル。",
      href: getDownloadUrl("/sample2.csv"),
      fileName: "sample_2.csv",
    },
  ];

  return (
    <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <div>
        <h2 className="text-xl font-bold text-slate-950">CSVダウンロード</h2>
        <p className="mt-2 text-sm leading-6 text-slate-600">
          テンプレートやサンプルを取得し、変換機能の動作確認に使用する。
        </p>
      </div>

      <div className="mt-5 space-y-3">
        {links.map((link) => (
          <a
            key={link.href}
            href={link.href}
            download={link.fileName}
            className="block rounded-xl border border-slate-200 bg-slate-50 p-4 transition hover:border-slate-300 hover:bg-slate-100"
          >
            <span className="block text-sm font-bold text-slate-900">
              {link.label}
            </span>
            <span className="mt-1 block text-xs leading-5 text-slate-500">
              {link.description}
            </span>
          </a>
        ))}
      </div>
    </section>
  );
}
