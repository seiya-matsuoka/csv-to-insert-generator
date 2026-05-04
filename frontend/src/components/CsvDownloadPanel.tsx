import { getDownloadUrl } from "../lib/api";

export function CsvDownloadPanel() {
  const links = [
    {
      label: "テンプレート",
      href: getDownloadUrl("/template.csv"),
      fileName: "template.csv",
    },
    {
      label: "サンプル①",
      href: getDownloadUrl("/sample1.csv"),
      fileName: "sample_1.csv",
    },
    {
      label: "サンプル②",
      href: getDownloadUrl("/sample2.csv"),
      fileName: "sample_2.csv",
    },
  ];

  return (
    <section className="rounded-2xl border border-slate-200 bg-white px-5 py-4 shadow-sm">
      <div className="flex flex-col gap-3">
        <div>
          <h2 className="text-sm font-bold text-slate-950">CSVダウンロード</h2>
          <p className="mt-1 text-xs leading-5 text-slate-600">
            入力形式確認や動作確認に使用する。
          </p>
        </div>

        <div className="grid grid-cols-3 gap-2">
          {links.map((link) => (
            <a
              key={link.href}
              href={link.href}
              download={link.fileName}
              className="rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-center text-xs font-bold text-slate-800 transition hover:border-slate-300 hover:bg-slate-100"
            >
              {link.label}
            </a>
          ))}
        </div>
      </div>
    </section>
  );
}
