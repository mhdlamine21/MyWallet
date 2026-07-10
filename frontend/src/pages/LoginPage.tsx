import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { authApi } from "../api/endpoints";
import { useAuthStore } from "../store/authStore";
import { useI18nStore } from "../i18n/useI18n";
import UserGuideModal from "../components/UserGuideModal";
import Logo from "../components/Logo";

const DEMO_ACCOUNTS = [
  { label: "Investisseur", email: "demo.investor@mywallet.dev", role: "INVESTOR", icon: "💎" },
  { label: "Trader", email: "demo.trader@mywallet.dev", role: "TRADER", icon: "⚡" },
  { label: "Analyste", email: "demo.analyst@mywallet.dev", role: "ANALYST", icon: "📊" },
  { label: "Administrateur", email: "demo.admin@mywallet.dev", role: "ADMIN", icon: "🛡️" },
];

export default function LoginPage() {
  const { lang, setLang, t } = useI18nStore();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [mode, setMode] = useState<"login" | "register">("login");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [isGuideOpen, setIsGuideOpen] = useState(false);
  const navigate = useNavigate();
  const setTokens = useAuthStore((s) => s.setTokens);
  const setUser = useAuthStore((s) => s.setUser);

  function fillDemoAccount(demoEmail: string) {
    setEmail(demoEmail);
    setPassword("demo-password-not-for-real-use");
    setError(null);
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const auth = mode === "login" ? await authApi.login(email, password) : await authApi.register(email, password);
      setTokens(auth.accessToken, auth.refreshToken);
      const profile = await authApi.me();
      setUser(profile);
      navigate("/dashboard");
    } catch {
      setError(mode === "login" ? t("login.error.invalid") : t("login.error.register"));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-[#091224] text-slate-100 p-4 relative overflow-hidden font-sans">
      {/* Ambient background glows */}
      <div className="absolute top-1/4 -left-20 w-96 h-96 bg-ocean-600/10 rounded-full blur-[120px] pointer-events-none" />
      <div className="absolute bottom-1/4 -right-20 w-96 h-96 bg-ocean-400/10 rounded-full blur-[120px] pointer-events-none" />

      <div className="w-full max-w-md rounded-2xl bg-[#0d1933]/90 backdrop-blur-xl border border-ocean-500/20 p-8 shadow-[0_16px_40px_0_rgba(0,0,0,0.45)] relative z-10">
        {/* Top bar: Lang toggle & Guide button */}
        <div className="flex items-center justify-between mb-6">
          <button
            type="button"
            onClick={() => setIsGuideOpen(true)}
            className="flex items-center gap-1.5 text-xs text-ocean-300 hover:text-white font-medium px-2.5 py-1 rounded-lg bg-navy-850 border border-ocean-500/30 hover:border-ocean-500/60 transition-all cursor-pointer"
          >
            <span>📖</span>
            <span>{lang === "fr" ? "Guide du projet" : "Project Guide"}</span>
          </button>

          <div className="flex gap-1 bg-navy-850 p-1 rounded-lg border border-ocean-500/20">
            <button
              type="button"
              onClick={() => setLang("fr")}
              className={`px-2 py-0.5 text-xs font-semibold rounded cursor-pointer ${
                lang === "fr" ? "bg-ocean-600 text-white" : "text-slate-400 hover:text-white"
              }`}
            >
              FR 🇫🇷
            </button>
            <button
              type="button"
              onClick={() => setLang("en")}
              className={`px-2 py-0.5 text-xs font-semibold rounded cursor-pointer ${
                lang === "en" ? "bg-ocean-600 text-white" : "text-slate-400 hover:text-white"
              }`}
            >
              EN 🇬🇧
            </button>
          </div>
        </div>

        {/* Brand Header */}
        <div className="text-center mb-6">
          <div className="flex justify-center mb-4">
            <Logo variant="full" size="lg" />
          </div>
          <h1 className="text-xl font-bold text-white">
            {t("login.title")}
          </h1>
          <p className="text-xs text-slate-400 mt-1 max-w-xs mx-auto">
            {t("login.subtitle")}
          </p>
        </div>

        {/* Login Form */}
        <form onSubmit={handleSubmit} className="space-y-3.5">
          <div>
            <label className="block text-[11px] font-medium text-slate-400 mb-1">
              {t("login.email")}
            </label>
            <input
              type="email"
              placeholder="votre.email@domaine.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full rounded-xl bg-[#132347] border border-ocean-500/20 focus:border-ocean-400 focus:ring-1 focus:ring-ocean-400 px-3.5 py-2.5 text-xs text-white placeholder-slate-400 outline-none transition-all"
              required
            />
          </div>

          <div>
            <label className="block text-[11px] font-medium text-slate-400 mb-1">
              {t("login.password")}
            </label>
            <input
              type="password"
              placeholder="••••••••••••"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full rounded-xl bg-[#132347] border border-ocean-500/20 focus:border-ocean-400 focus:ring-1 focus:ring-ocean-400 px-3.5 py-2.5 text-xs text-white placeholder-slate-400 outline-none transition-all"
              required
              minLength={12}
            />
          </div>

          {error && (
            <div className="p-2.5 rounded-lg bg-red-950/40 border border-red-500/30 text-red-300 text-xs flex items-center gap-2">
              <span>⚠️</span>
              <span>{error}</span>
            </div>
          )}

          <button
            type="submit"
            disabled={loading}
            className="w-full rounded-xl bg-ocean-600 hover:bg-ocean-500 px-4 py-2.5 text-xs font-semibold text-white shadow-lg shadow-ocean-600/30 active:scale-[0.99] disabled:opacity-50 transition-all cursor-pointer"
          >
            {loading ? t("login.loading") : mode === "login" ? t("login.signin") : t("login.createAccount")}
          </button>
        </form>

        {/* Toggle Mode */}
        <div className="text-center mt-3">
          <button
            type="button"
            className="text-xs text-ocean-400 hover:text-ocean-300 transition-colors cursor-pointer"
            onClick={() => setMode(mode === "login" ? "register" : "login")}
          >
            {mode === "login" ? t("login.newHere") : t("login.alreadyAccount")}
          </button>
        </div>

        {/* Demo Accounts Section */}
        <div className="mt-6 pt-5 border-t border-ocean-500/20">
          <p className="text-xs font-medium text-slate-300 mb-2.5 flex items-center gap-1.5">
            <span>✨</span> {t("login.demoAccounts")}
          </p>
          <div className="grid grid-cols-2 gap-2 mb-3">
            {DEMO_ACCOUNTS.map((acc) => (
              <button
                key={acc.email}
                type="button"
                onClick={() => fillDemoAccount(acc.email)}
                className="flex items-center gap-2 text-left p-2.5 rounded-xl bg-[#132347] border border-ocean-500/20 hover:border-ocean-400/50 hover:bg-[#1a2f5e] transition-all group cursor-pointer"
              >
                <span className="text-base group-hover:scale-110 transition-transform">{acc.icon}</span>
                <div className="overflow-hidden">
                  <div className="text-[11px] font-semibold text-slate-200 group-hover:text-white truncate">
                    {acc.label}
                  </div>
                  <div className="text-[10px] text-ocean-300 truncate">{acc.role}</div>
                </div>
              </button>
            ))}
          </div>
          <p className="text-[10px] text-slate-400 text-center font-mono">
            {t("login.demoSharedPassword")} <code className="text-ocean-300">demo-password-not-for-real-use</code>
          </p>
        </div>
      </div>

      <UserGuideModal isOpen={isGuideOpen} onClose={() => setIsGuideOpen(false)} />
    </div>
  );
}
