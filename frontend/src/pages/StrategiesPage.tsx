import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { portfoliosApi, strategiesApi } from "../api/endpoints";
import type { ApiError, LeaderboardEntry } from "../api/types";
import { isAxiosError } from "axios";
import { useStompTopic } from "../hooks/useStompTopic";
import { useI18nStore } from "../i18n/useI18n";
import { useCurrencyStore } from "../store/useCurrencyStore";
import UserGuideModal from "../components/UserGuideModal";

const EXAMPLE_RULE = "SMA(BTCUSDT, 20) > SMA(BTCUSDT, 50) AND RSI(BTCUSDT, 14) < 70 AND PortfolioExposure < 50%";

export default function StrategiesPage() {
  const queryClient = useQueryClient();
  const { lang, t } = useI18nStore();
  const { formatMoney } = useCurrencyStore();
  const [isGuideOpen, setIsGuideOpen] = useState(false);

  const { data: strategies, isLoading } = useQuery({ queryKey: ["strategies"], queryFn: strategiesApi.list });
  const { data: portfolios } = useQuery({ queryKey: ["portfolios"], queryFn: portfoliosApi.list });
  const { data: initialLeaderboard } = useQuery({ queryKey: ["leaderboard"], queryFn: strategiesApi.leaderboard });
  const [leaderboard, setLeaderboard] = useState<LeaderboardEntry[]>([]);

  useStompTopic<LeaderboardEntry[]>("/topic/leaderboard", setLeaderboard);

  const rows = leaderboard.length > 0 ? leaderboard : (initialLeaderboard ?? []);

  const [showForm, setShowForm] = useState(false);
  const [name, setName] = useState("");
  const [portfolioId, setPortfolioId] = useState("");
  const [ruleExpression, setRuleExpression] = useState(EXAMPLE_RULE);
  const [ruleError, setRuleError] = useState<string | null>(null);

  const createMutation = useMutation({
    mutationFn: () => strategiesApi.create({ portfolioId, name, mode: "PAPER", ruleExpression }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["strategies"] });
      setShowForm(false);
      setRuleError(null);
      setName("");
    },
    onError: (err) => {
      if (isAxiosError<ApiError>(err) && err.response) {
        setRuleError(err.response.data.message);
      } else {
        setRuleError(lang === "fr" ? "Impossible de créer la stratégie." : "Could not create the strategy.");
      }
    },
  });

  return (
    <div className="p-4 md:p-8 max-w-7xl mx-auto space-y-6 md:space-y-8 animate-fadeIn">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 pb-6 border-b border-ocean-500/20">
        <div>
          <h1 className="text-2xl font-bold text-white">
            {t("strategies.title")}
          </h1>
          <p className="text-xs text-slate-400 mt-1">
            {t("strategies.subtitle")}
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={() => setIsGuideOpen(true)}
            className="flex items-center gap-2 px-3.5 py-2 rounded-xl bg-navy-850 border border-ocean-500/30 hover:border-ocean-500/60 text-ocean-300 text-xs font-medium transition-all shadow-sm"
          >
            <span>📖</span>
            <span>{lang === "fr" ? "Guide Moteur de Règles" : "Rule Engine Guide"}</span>
          </button>

          <button
            onClick={() => setShowForm(!showForm)}
            className="flex items-center gap-2 px-4 py-2 rounded-xl bg-ocean-600 hover:bg-ocean-500 text-white text-xs font-semibold shadow-lg shadow-ocean-600/30 transition-all cursor-pointer"
          >
            <span>{showForm ? "✕" : "+"}</span>
            <span>{showForm ? (lang === "fr" ? "Annuler" : "Cancel") : t("strategies.new")}</span>
          </button>
        </div>
      </div>

      {/* Creation Modal / Form Card */}
      {showForm && (
        <div className="rounded-2xl bg-[#0d1933] border border-ocean-500/30 p-6 max-w-2xl space-y-4 shadow-2xl relative overflow-hidden">
          <div className="absolute top-0 right-0 w-64 h-64 bg-ocean-600/10 rounded-full blur-3xl pointer-events-none" />

          <h3 className="text-sm font-semibold text-white flex items-center gap-2">
            <span>⚡</span>
            <span>{lang === "fr" ? "Nouvelle stratégie de trading algorithmique" : "New Algorithmic Trading Strategy"}</span>
          </h3>

          <div className="space-y-3.5">
            <div>
              <label className="block text-[11px] font-medium text-slate-400 mb-1">Portefeuille associé</label>
              <select
                value={portfolioId}
                onChange={(e) => setPortfolioId(e.target.value)}
                className="w-full rounded-xl bg-[#132347] border border-ocean-500/20 focus:border-ocean-400 focus:ring-1 focus:ring-ocean-400 px-3 py-2 text-xs text-white outline-none"
              >
                <option value="">Sélectionner un portefeuille…</option>
                {portfolios?.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.name} ({formatMoney(p.cashBalance)})
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-[11px] font-medium text-slate-400 mb-1">Nom de la stratégie</label>
              <input
                placeholder="ex: Momentum BTC RSI Reversal"
                value={name}
                onChange={(e) => setName(e.target.value)}
                className="w-full rounded-xl bg-[#132347] border border-ocean-500/20 focus:border-ocean-400 focus:ring-1 focus:ring-ocean-400 px-3.5 py-2.5 text-xs text-white outline-none"
              />
            </div>

            <div>
              <div className="flex justify-between items-center mb-1">
                <label className="block text-[11px] font-medium text-slate-400">
                  {t("strategies.ruleExpression")}
                </label>
                <span className="text-[10px] text-ocean-400 font-mono">AST Grammar Safe</span>
              </div>
              <textarea
                rows={3}
                value={ruleExpression}
                onChange={(e) => setRuleExpression(e.target.value)}
                className="w-full rounded-xl bg-[#132347] border border-ocean-500/20 focus:border-ocean-400 focus:ring-1 focus:ring-ocean-400 p-3 text-xs text-emerald-400 font-mono outline-none"
              />
              <p className="text-[10px] text-slate-400 mt-1">
                Indicateurs supportés : <code className="text-ocean-300">SMA(symbole, période)</code>, <code className="text-ocean-300">RSI(symbole, période)</code>, <code className="text-ocean-300">PortfolioExposure</code>
              </p>
            </div>

            {ruleError && (
              <div className="p-2.5 rounded-lg bg-red-950/40 border border-red-500/30 text-red-300 text-xs">
                {ruleError}
              </div>
            )}

            <button
              onClick={() => createMutation.mutate()}
              disabled={!portfolioId || !name || !ruleExpression || createMutation.isPending}
              className="w-full rounded-xl bg-ocean-600 hover:bg-ocean-500 px-4 py-2.5 text-xs font-semibold text-white shadow-lg shadow-ocean-600/30 disabled:opacity-50 transition-all cursor-pointer"
            >
              {createMutation.isPending ? "Validation de la règle…" : t("strategies.createBtn")}
            </button>
          </div>
        </div>
      )}

      {/* Grid: Strategies List & Live Leaderboard */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left 2 Cols: Strategies */}
        <div className="lg:col-span-2 space-y-4">
          <h2 className="text-sm font-semibold text-white">
            {lang === "fr" ? "Stratégies enregistrées" : "Registered Strategies"}
          </h2>

          {isLoading && (
            <div className="p-8 text-center text-xs text-slate-500">
              <div className="w-6 h-6 mx-auto mb-2 border-2 border-ocean-500 border-t-transparent rounded-full animate-spin" />
              Chargement des stratégies…
            </div>
          )}

          {strategies && strategies.length === 0 && (
            <div className="p-8 text-center text-xs text-slate-500 border border-dashed border-ocean-500/20 rounded-2xl">
              {lang === "fr" ? "Aucune stratégie enregistrée pour l'instant." : "No strategies registered yet."}
            </div>
          )}

          {strategies && strategies.length > 0 && (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {strategies.map((s) => (
                <Link
                  key={s.id}
                  to={`/strategies/${s.id}`}
                  className="p-5 rounded-2xl bg-[#0d1933] border border-ocean-500/20 hover:border-ocean-500/50 hover:bg-[#132347] transition-all group shadow-md"
                >
                  <div className="flex justify-between items-start mb-2">
                    <div>
                      <h3 className="font-semibold text-white group-hover:text-ocean-300 transition-colors text-sm">
                        {s.name}
                      </h3>
                      <p className="text-[10px] text-slate-500 font-mono mt-0.5">
                        ID: {s.id.slice(0, 8)}…
                      </p>
                    </div>
                    <span
                      className={`text-[10px] font-mono px-2 py-0.5 rounded-full border ${
                        s.status === "ACTIVE"
                          ? "bg-emerald-950/50 text-emerald-400 border-emerald-500/30"
                          : s.status === "SUSPENDED"
                          ? "bg-amber-950/50 text-amber-400 border-amber-500/30"
                          : "bg-navy-850 text-slate-400 border-slate-700"
                      }`}
                    >
                      {s.status}
                    </span>
                  </div>

                  <div className="p-2.5 rounded-xl bg-[#080e1e] border border-ocean-500/10 my-3">
                    <p className="text-[11px] font-mono text-emerald-400/90 truncate">
                      {s.ruleExpression}
                    </p>
                  </div>

                  <div className="flex items-center justify-between text-xs text-ocean-400 font-medium group-hover:translate-x-1 transition-transform">
                    <span>{lang === "fr" ? "Détails & Backtest ->" : "Details & Backtest ->"}</span>
                  </div>
                </Link>
              ))}
            </div>
          )}
        </div>

        {/* Right Col: Live Leaderboard */}
        <div className="space-y-4">
          <div className="rounded-2xl bg-[#0d1933] border border-ocean-500/20 p-5 shadow-xl">
            <h2 className="text-sm font-semibold text-white mb-1 flex items-center justify-between">
              <span className="flex items-center gap-2">
                <span>🏆</span>
                <span>{t("strategies.leaderboard")}</span>
              </span>
              <span className="text-[10px] font-mono text-ocean-300 bg-navy-850 px-2 py-0.5 rounded-full border border-ocean-500/30">
                LIVE
              </span>
            </h2>
            <p className="text-[11px] text-slate-400 mb-4">
              {lang === "fr" ? "Mise à jour en temps réel par WebSocket" : "Real-time updates via WebSocket"}
            </p>

            {rows.length === 0 ? (
              <p className="text-xs text-slate-500">{lang === "fr" ? "Aucune donnée de classement pour le moment." : "No leaderboard data yet."}</p>
            ) : (
              <div className="space-y-2.5">
                {rows.map((row, idx) => (
                  <div
                    key={row.strategyId}
                    className="p-3 rounded-xl bg-[#132347] border border-ocean-500/20 flex items-center justify-between text-xs"
                  >
                    <div className="flex items-center gap-2.5">
                      <span className="text-base font-bold">
                        {idx === 0 ? "🥇" : idx === 1 ? "🥈" : idx === 2 ? "🥉" : `#${idx + 1}`}
                      </span>
                      <div>
                        <div className="font-semibold text-white">{row.strategyName}</div>
                        <div className="text-[10px] text-ocean-300 font-mono">
                          Valeur: {formatMoney(row.currentValue)}
                        </div>
                      </div>
                    </div>
                    <div className="text-right">
                      <div className={`font-mono font-bold ${Number(row.returnFraction) >= 0 ? "text-emerald-400" : "text-rose-400"}`}>
                        {Number(row.returnFraction) >= 0 ? "+" : ""}{(Number(row.returnFraction) * 100).toFixed(1)}%
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>

      <UserGuideModal isOpen={isGuideOpen} onClose={() => setIsGuideOpen(false)} />
    </div>
  );
}
