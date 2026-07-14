import { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import { useI18nStore } from "../i18n/useI18n";

export default function CookieBanner() {
  const { lang } = useI18nStore();
  const [isVisible, setIsVisible] = useState(false);

  useEffect(() => {
    const consent = localStorage.getItem("mywallet_cookie_consent");
    if (!consent) {
      setIsVisible(true);
    }
  }, []);

  const handleConsent = (choice: "accepted" | "declined") => {
    localStorage.setItem("mywallet_cookie_consent", choice);
    setIsVisible(false);
  };

  if (!isVisible) return null;

  return (
    <div className="fixed bottom-4 left-4 right-4 md:left-8 md:right-auto md:max-w-md z-50 animate-fadeIn">
      <div className="p-4 rounded-2xl bg-[#0F121A]/95 border border-white/10 shadow-[0_10px_35px_-5px_rgba(0,0,0,0.8)] backdrop-blur-md text-slate-200 space-y-3">
        <div className="flex items-start gap-3">
          <div className="w-8 h-8 rounded-lg bg-indigo-600/20 text-indigo-400 flex items-center justify-center shrink-0">
            🍪
          </div>
          <div className="text-xs leading-relaxed space-y-1">
            <p className="font-semibold text-white">
              {lang === "fr" ? "Gestion des cookies & données" : "Cookie & Data preferences"}
            </p>
            <p className="text-slate-400">
              {lang === "fr"
                ? "Nous utilisons des cookies strictement nécessaires au maintien de session et aux préférences de langue. Aucun traceur publicitaire n'est utilisé."
                : "We use strictly necessary cookies for session persistence and language preferences. No ad trackers are used."}
            </p>
          </div>
        </div>

        <div className="flex items-center justify-between pt-1">
          <Link
            to="/privacy"
            className="text-[11px] text-indigo-400 hover:text-indigo-300 underline underline-offset-2"
          >
            {lang === "fr" ? "Politique de confidentialité" : "Privacy Policy"}
          </Link>
          <div className="flex items-center gap-2">
            <button
              onClick={() => handleConsent("declined")}
              className="px-3 py-1.5 text-xs text-slate-400 hover:text-white rounded-lg hover:bg-white/5 transition-colors"
            >
              {lang === "fr" ? "Refuser" : "Decline"}
            </button>
            <button
              onClick={() => handleConsent("accepted")}
              className="px-3.5 py-1.5 text-xs font-semibold text-white bg-indigo-600 hover:bg-indigo-500 rounded-lg shadow-md shadow-indigo-600/30 transition-all"
            >
              {lang === "fr" ? "Accepter" : "Accept"}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
