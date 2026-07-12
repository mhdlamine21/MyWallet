import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { accountsApi, portfoliosApi } from "../api/endpoints";
import { useI18nStore } from "../i18n/useI18n";
import { useCurrencyStore } from "../store/useCurrencyStore";
import UserGuideModal from "../components/UserGuideModal";

export default function PortfoliosPage() {
  const { lang, t } = useI18nStore();
  const { formatMoney } = useCurrencyStore();
  const queryClient = useQueryClient();
  const [isGuideOpen, setIsGuideOpen] = useState(false);

  const { data: portfolios, isLoading } = useQuery({ queryKey: ["portfolios"], queryFn: portfoliosApi.list });
  const { data: accounts } = useQuery({ queryKey: ["accounts"], queryFn: accountsApi.list });

  const [showForm, setShowForm] = useState(false);
  const [name, setName] = useState("");
  const [mode, setMode] = useState("SIMULATED");
  const [initialCash, setInitialCash] = useState("10000");
  const [accountId, setAccountId] = useState<string>("");

  const createAccountMutation = useMutation({
    mutationFn: () => accountsApi.create("SIMULATED", "Compte Simulé Principal"),
    onSuccess: (account) => {
      queryClient.invalidateQueries({ queryKey: ["accounts"] });
      setAccountId(account.id);
    },
  });

  const createPortfolioMutation = useMutation({
    mutationFn: () => portfoliosApi.create(accountId, name, mode, initialCash),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["portfolios"] });
      setShowForm(false);
      setName("");
      setInitialCash("10000");
    },
  });

  return (
    <div className="p-4 md:p-8 max-w-7xl mx-auto space-y-6 md:space-y-8 animate-fadeIn">
      {/* Top Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 pb-6 border-b border-white/[0.08]">
        <div>
          <h1 className="text-2xl font-bold text-white tracking-tight">
            {t("portfolios.title")}
          </h1>
          <p className="text-xs text-slate-400 mt-1">
            {t("portfolios.subtitle")}
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2.5">
          <button
            onClick={() => setIsGuideOpen(true)}
            className="flex items-center gap-2 px-3.5 py-2 rounded-xl bg-[#0D1933] border border-ocean-500/20 hover:border-ocean-500/40 text-ocean-300 text-xs font-medium transition-all shadow-sm"
          >
            <span>📖</span>
            <span>{lang === "fr" ? "Guide Portefeuilles" : "Portfolios Guide"}</span>
          </button>

          <button
            onClick={() => setShowForm(!showForm)}
            className="flex items-center gap-2 px-4 py-2 rounded-xl bg-ocean-600 hover:bg-ocean-500 text-white text-xs font-semibold shadow-md shadow-sky-950/40 transition-all cursor-pointer"
          >
            <span>{showForm ? "✕" : "+"}</span>
            <span>{showForm ? (lang === "fr" ? "Annuler" : "Cancel") : t("portfolios.new")}</span>
          </button>
        </div>
      </div>

      {/* Creation Modal / Card */}
      {showForm && (
        <div className="rounded-2xl bg-[#0D1933] border border-ocean-500/30 p-6 max-w-xl space-y-4 shadow-2xl relative overflow-hidden">
          <h3 className="text-sm font-semibold text-white flex items-center gap-2">
            <span>✨</span>
            <span>{lang === "fr" ? "Création d'un portefeuille de simulation" : "Create Simulated Portfolio"}</span>
          </h3>

          {(!accounts || accounts.length === 0) && (
            <div className="p-4 rounded-xl bg-amber-950/30 border border-amber-500/30 space-y-2">
              <p className="text-xs text-amber-300">{t("portfolios.needAccount")}</p>
              <button
                onClick={() => createAccountMutation.mutate()}
                disabled={createAccountMutation.isPending}
                className="px-3.5 py-2 rounded-lg bg-amber-500/20 hover:bg-amber-500/30 border border-amber-500/40 text-amber-200 text-xs font-medium cursor-pointer"
              >
                {createAccountMutation.isPending ? "Création…" : t("portfolios.createAccount")}
              </button>
            </div>
          )}

          {accounts && accounts.length > 0 && (
            <div className="space-y-3.5">
              <div>
                <label className="block text-[11px] font-medium text-slate-400 mb-1">Compte de rattachement</label>
                <select
                  value={accountId}
                  onChange={(e) => setAccountId(e.target.value)}
                  className="w-full rounded-xl bg-[#091224] border border-white/10 px-3.5 py-2.5 text-xs text-slate-100 focus:border-ocean-500 focus:outline-none"
                >
                  <option value="">Sélectionnez un compte…</option>
                  {accounts.map((acc) => (
                    <option key={acc.id} value={acc.id}>
                      {acc.displayName} ({acc.accountType})
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-[11px] font-medium text-slate-400 mb-1">{t("portfolios.name")}</label>
                <input
                  type="text"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="Ex: Portefeuille Rendement US / Tech"
                  className="w-full rounded-xl bg-[#091224] border border-white/10 px-3.5 py-2.5 text-xs text-slate-100 placeholder-slate-500 focus:border-ocean-500 focus:outline-none"
                />
              </div>

              <div>
                <label className="block text-[11px] font-medium text-slate-400 mb-1">{t("portfolios.mode")}</label>
                <select
                  value={mode}
                  onChange={(e) => setMode(e.target.value)}
                  className="w-full rounded-xl bg-[#091224] border border-white/10 px-3.5 py-2.5 text-xs text-slate-100 focus:border-ocean-500 focus:outline-none"
                >
                  <option value="SIMULATED">{t("portfolios.mode.simulated")}</option>
                  <option value="PAPER">{t("portfolios.mode.paper")}</option>
                </select>
              </div>

              <div>
                <label className="block text-[11px] font-medium text-slate-400 mb-1">
                  {t("portfolios.initialCash")} (USD brut de base)
                </label>
                <input
                  type="number"
                  value={initialCash}
                  onChange={(e) => setInitialCash(e.target.value)}
                  className="w-full rounded-xl bg-[#091224] border border-white/10 px-3.5 py-2.5 text-xs text-slate-100 font-mono focus:border-ocean-500 focus:outline-none"
                />
                <p className="text-[10px] text-ocean-400 mt-1">
                  Équivalent dans la devise d'affichage : {formatMoney(Number(initialCash) || 0)}
                </p>
              </div>

              <button
                type="button"
                onClick={() => createPortfolioMutation.mutate()}
                disabled={!accountId || !name || createPortfolioMutation.isPending}
                className="w-full rounded-xl bg-ocean-600 hover:bg-ocean-500 px-4 py-2.5 text-xs font-semibold text-white shadow-md shadow-sky-950/40 disabled:opacity-50 transition-all cursor-pointer"
              >
                {createPortfolioMutation.isPending ? "Création en cours…" : t("portfolios.createBtn")}
              </button>
            </div>
          )}
        </div>
      )}

      {/* Portfolios Cards Grid */}
      {isLoading ? (
        <div className="p-12 text-center text-xs text-slate-400">
          <div className="w-8 h-8 mx-auto mb-3 border-2 border-ocean-400 border-t-transparent rounded-full animate-spin" />
          Chargement des portefeuilles…
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 md:gap-5">
          {portfolios?.map((p) => (
            <Link
              key={p.id}
              to={`/portfolios/${p.id}`}
              className="p-5 rounded-2xl bg-[#0D1933] border border-white/[0.08] hover:border-ocean-400/50 hover:bg-[#132347] transition-all group shadow-md"
            >
              <div className="flex justify-between items-start mb-3">
                <div>
                  <h3 className="font-semibold text-white group-hover:text-ocean-300 transition-colors text-sm">
                    {p.name}
                  </h3>
                  <p className="text-[10px] text-slate-400 font-mono mt-0.5">
                    {p.mode} · ID: {p.id.slice(0, 8)}…
                  </p>
                </div>
                <span className="text-[10px] font-mono rounded-full bg-ocean-500/10 text-ocean-400 border border-ocean-500/20 px-2.5 py-0.5">
                  {p.mode}
                </span>
              </div>

              <div className="pt-3 border-t border-white/[0.05]">
                <p className="text-[11px] text-slate-400 mb-1">{t("dashboard.availableCash")}</p>
                <p className="text-xl font-bold font-mono text-white">
                  {formatMoney(p.cashBalance)}
                </p>
              </div>

              <div className="mt-4 flex items-center justify-between text-xs text-ocean-400 font-medium group-hover:translate-x-1 transition-transform">
                <span>{t("dashboard.viewDetail")}</span>
              </div>
            </Link>
          ))}
        </div>
      )}

      <UserGuideModal isOpen={isGuideOpen} onClose={() => setIsGuideOpen(false)} />
    </div>
  );
}
