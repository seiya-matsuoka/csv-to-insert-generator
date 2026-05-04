import { getDownloadUrl } from "../lib/api";

export function CsvDownloadPanel() {
  const links = [
    {
      label: "テンプレートCSV",
      description: "入力フォーマットの雛形",
      href: getDownloadUrl("/template.csv"),
      fileName: "template.csv",
    },
    {
      label: "サンプルCSV①",
      description: "基本的な値のサンプル",
      href: getDownloadUrl("/sample1.csv"),
      fileName: "sample_1.csv",
    },
    {
      label: "サンプルCSV②",
      description: "NULL / DEFAULT / 空文字の確認",
      href: getDownloadUrl("/sample2.csv"),
      fileName: "sample_2.csv",
    },
  ];

  return (
    <section className="h-full rounded-2xl border border-slate-200 bg-white px-5 py-4 shadow-sm">
      <div>
        <h2 className="text-sm font-bold text-slate-950">CSVダウンロード</h2>
        <p className="mt-1 text-xs leading-5 text-slate-600">
          テンプレートやサンプルを取得して、入力形式や動作を確認する。
        </p>
      </div>

      <div className="mt-4 grid gap-2 sm:grid-cols-3">
        {links.map((link) => (
          <a
            key={link.href}
            href={link.href}
            download={link.fileName}
            className="rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-center transition hover:border-slate-300 hover:bg-slate-100"
          >
            <span className="block text-xs font-bold text-slate-900">
              {link.label}
            </span>
            <span className="mt-1 block text-[11px] leading-4 text-slate-500">
              {link.description}
            </span>
          </a>
        ))}
      </div>
    </section>
  );
}
