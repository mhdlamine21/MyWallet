import { Link } from "react-router-dom";
import { useI18nStore } from "../i18n/useI18n";

export default function NotFoundPage() {
  const { lang } = useI18nStore();

  return (
    <div className="min-h-screen bg-[#08090C] text-slate-100 flex items-center justify-center p-6 selection:bg-indigo-500/30">
      <div className="max-w-md w-full text-center space-y-6 animate-fadeIn">
        <div className="relative inline-block">
          <div className="text-8xl font-black text-transparent bg-clip-text bg-gradient-to-r from-indigo-500 via-purple-500 to-pink-500 font-mono">
            404
          </div>
          <div className="absolute -bottom-2 left-1/2 -translate-x-1/2 px-3 py-0.5 rounded-full bg-indigo-950/80 border border-indigo-500/30 text-indigo-400 text-xs font-semibold">
            {lang === "fr" ? "Page Introuvable" : "Page Not Found"}
          </div>
        </div>

        <div className="space-y-2">
          <h1 className="text-xl font-bold text-white">
            {lang === "fr" ? "Oups ! Cette destination n'existe pas" : "Oops! This route does not exist"}
          </h1>
          <p className="text-xs text-slate-400 leading-relaxed">
            {lang === "fr"
              ? "Le lien que vous avez suivi est peut-être erroné ou la ressource a été déplacée dans le simulateur."
              : "The link you followed may be broken or the resource has been relocated."}
          </p>
        </div>

        <div className="flex flex-col sm:flex-row items-center justify-center gap-3 pt-4">
          <Link
            to="/dashboard"
            className="w-full sm:w-auto px-5 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold shadow-lg shadow-indigo-600/30 transition-all"
          >
            {lang === "fr" ? "🏠 Retour au Dashboard" : "🏠 Back to Dashboard"}
          </Link>
          <Link
            to="/portfolios"
            className="w-full sm:w-auto px-5 py-2.5 rounded-xl bg-white/5 hover:bg-white/10 text-slate-300 hover:text-white border border-white/10 text-xs font-semibold transition-all"
          >
            {lang === "fr" ? "💼 Voir mes Portefeuilles" : "💼 View Portfolios"}
          </Link>
        </div>
      </div>
    </div>
  );
}
