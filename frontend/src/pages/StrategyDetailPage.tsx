import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useParams, Link } from "react-router-dom";
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from "recharts";
import { backtestApi, strategiesApi, assetsApi } from "../api/endpoints";
import type { BacktestResult } from "../api/types";
import { useI18nStore } from "../i18n/useI18n";
import { useCurrencyStore } from "../store/useCurrencyStore";
import UserGuideModal from "../components/UserGuideModal";

export default function StrategyDetailPage() {
  const { id } = useParams<{ id: string }>();
  const strategyId = id!;
  const queryClient = useQueryClient();
  const { lang, t } = useI18nStore();
  const { currency, formatMoney } = useCurrencyStore();
  const [isGuideOpen, setIsGuideOpen] = useState(false);

  const { data: strategy } = useQuery({ queryKey: ["strategy", strategyId], queryFn: () => strategiesApi.get(strategyId) });
  const { data: assets } = useQuery({ queryKey: ["assets"], queryFn: assetsApi.list });

  const [assetId, setAssetId] = useState("");
  const [initialCapital, setInitialCapital] = useState("10000");
  const [periodStart, setPeriodStart] = useState(daysAgoIso(30));
  const [periodEnd, setPeriodEnd] = useState(daysAgoIso(0));
  const [result, setResult] = useState<BacktestResult | null>(null);
  const [backtestError, setBacktestError] = useState<string | null>(null);

  const activateMutation = useMutation({
    mutationFn: () => strategiesApi.activate(strategyId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["strategy", strategyId] }),
  });
  const suspendMutation = useMutation({
    mutationFn: () => strategiesApi.suspend(strategyId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["strategy", strategyId] }),
  });
  const deactivateMutation = useMutation({
    mutationFn: () => strategiesApi.deactivate(strategyId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["strategy", strategyId] }),
  });

  const backtestMutation = useMutation({
    mutationFn: () =>
      backtestApi.run(strategyId, {
        assetId,
        initialCapital,
        periodStart: new Date(periodStart).toISOString(),
        periodEnd: new Date(periodEnd).toISOString(),
      }),
    onSuccess: (r) => {
      setResult(r);
      setBacktestError(null);
    },
    onError: () =>
      setBacktestError(
        lang === "fr"
          ? "Échec du backtest : vérifiez que l'actif est sélectionné et qu'il dispose de suffisamment d'historique."
          : "Backtest failed - check the asset and ensure enough price history exists for the selected period."
      ),
  });

  const chartData = result
    ? result.equityCurve.map((v, i) => ({
        tick: i,
        strategy: v,
        buyAndHold: result.buyAndHoldEquityCurve[i],
        p5: result.monteCarloP5[i],
        p50: result.monteCarloP50[i],
        p95: result.monteCarloP95[i],
      }))
    : [];

  if (!strategy) return null;

  return (
    <div className="p-4 md:p-8 max-w-7xl mx-auto space-y-6 md:space-y-8 animate-fadeIn">
      {/* Breadcrumb & Navigation */}
      <div className="flex items-center justify-between pb-4 border-b border-ocean-500/20">
        <div className="flex items-center gap-3">
          <Link to="/strategies" className="text-xs text-slate-400 hover:text-ocean-300 transition-colors">
            &larr; {t("nav.strategies")}
          </Link>
          <span className="text-slate-600">/</span>
          <span className="text-xs text-white font-medium">{strategy.name}</span>
        </div>

        <button
          onClick={() => setIsGuideOpen(true)}
          className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-navy-850 border border-ocean-500/30 text-ocean-300 text-xs font-medium hover:border-ocean-500/60"
        >
          <span>📖</span>
          <span>{lang === "fr" ? "Manuel Backtest & Monte Carlo" : "Backtest & Monte Carlo Guide"}</span>
        </button>
      </div>

      {/* Main Strategy Header */}
      <div className="p-6 rounded-2xl bg-[#0d1933] border border-ocean-500/20 shadow-xl flex flex-col md:flex-row md:items-center justify-between gap-6 relative overflow-hidden">
        <div className="absolute top-0 right-0 w-64 h-64 bg-ocean-600/10 rounded-full blur-3xl pointer-events-none" />

        <div className="space-y-2">
          <div className="flex items-center gap-3">
            <h1 className="text-2xl font-bold text-white tracking-tight">{strategy.name}</h1>
            <span
              className={`text-[10px] font-mono px-2.5 py-0.5 rounded-full border ${
                strategy.status === "ACTIVE"
                  ? "bg-emerald-950/50 text-emerald-400 border-emerald-500/30"
                  : strategy.status === "SUSPENDED"
                  ? "bg-amber-950/50 text-amber-400 border-amber-500/30"
                  : "bg-navy-850 text-slate-400 border-slate-700"
              }`}
            >
              {strategy.status}
            </span>
          </div>

          <div className="p-3 rounded-xl bg-[#080e1e] border border-ocean-500/10 font-mono text-xs text-emerald-400 max-w-2xl">
            {strategy.ruleExpression}
          </div>
        </div>

        {/* Status Action Buttons */}
        <div className="flex gap-2 shrink-0">
          {strategy.status === "DRAFT" && (
            <button
              onClick={() => activateMutation.mutate()}
              className="px-4 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-semibold shadow-md shadow-emerald-600/20 transition-all cursor-pointer"
            >
              {t("strategies.activate")}
            </button>
          )}
          {strategy.status === "ACTIVE" && (
            <>
              <button
                onClick={() => suspendMutation.mutate()}
                className="px-3.5 py-2 rounded-xl bg-amber-600/20 hover:bg-amber-600/30 border border-amber-500/40 text-amber-200 text-xs font-semibold transition-all cursor-pointer"
              >
                {t("strategies.suspend")}
              </button>
              <button
                onClick={() => deactivateMutation.mutate()}
                className="px-3.5 py-2 rounded-xl bg-rose-600/20 hover:bg-rose-600/30 border border-rose-500/40 text-rose-300 text-xs font-semibold transition-all cursor-pointer"
              >
                {t("strategies.deactivate")}
              </button>
            </>
          )}
          {strategy.status === "SUSPENDED" && (
            <>
              <button
                onClick={() => activateMutation.mutate()}
                className="px-4 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-semibold shadow-md shadow-emerald-600/20 transition-all cursor-pointer"
              >
                {lang === "fr" ? "Réactiver" : "Reactivate"}
              </button>
              <button
                onClick={() => deactivateMutation.mutate()}
                className="px-3.5 py-2 rounded-xl bg-rose-600/20 hover:bg-rose-600/30 border border-rose-500/40 text-rose-300 text-xs font-semibold transition-all cursor-pointer"
              >
                {t("strategies.deactivate")}
              </button>
            </>
          )}
        </div>
      </div>

      {/* Backtest Configuration */}
      <div className="rounded-2xl bg-[#0d1933] border border-ocean-500/20 p-6 shadow-xl space-y-4">
        <h2 className="text-sm font-semibold text-white flex items-center gap-2">
          <span>📈</span>
          <span>{t("strategies.backtest")}</span>
        </h2>

        <div className="grid grid-cols-1 md:grid-cols-4 gap-3.5">
          <div>
            <label className="block text-[11px] font-medium text-slate-400 mb-1">{t("portfolioDetail.asset")}</label>
            <select
              value={assetId}
              onChange={(e) => setAssetId(e.target.value)}
              className="w-full rounded-xl bg-[#132347] border border-ocean-500/20 focus:border-ocean-400 focus:ring-1 focus:ring-ocean-400 px-3 py-2 text-xs text-white outline-none"
            >
              <option value="">Sélectionner un actif…</option>
              {assets?.map((a) => (
                <option key={a.id} value={a.id}>
                  {a.symbol} - {a.displayName}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-[11px] font-medium text-slate-400 mb-1">
              {t("strategies.initialCapital")} ({currency})
            </label>
            <input
              value={initialCapital}
              onChange={(e) => setInitialCapital(e.target.value)}
              type="number"
              className="w-full rounded-xl bg-[#132347] border border-ocean-500/20 focus:border-ocean-400 focus:ring-1 focus:ring-ocean-400 px-3 py-2 text-xs text-white outline-none font-mono"
            />
          </div>

          <div>
            <label className="block text-[11px] font-medium text-slate-400 mb-1">Date début</label>
            <input
              type="date"
              value={periodStart}
              onChange={(e) => setPeriodStart(e.target.value)}
              className="w-full rounded-xl bg-[#132347] border border-ocean-500/20 focus:border-ocean-400 focus:ring-1 focus:ring-ocean-400 px-3 py-2 text-xs text-white outline-none"
            />
          </div>

          <div>
            <label className="block text-[11px] font-medium text-slate-400 mb-1">Date fin</label>
            <input
              type="date"
              value={periodEnd}
              onChange={(e) => setPeriodEnd(e.target.value)}
              className="w-full rounded-xl bg-[#132347] border border-ocean-500/20 focus:border-ocean-400 focus:ring-1 focus:ring-ocean-400 px-3 py-2 text-xs text-white outline-none"
            />
          </div>
        </div>

        {backtestError && (
          <div className="p-3 rounded-xl bg-red-950/40 border border-red-500/30 text-red-300 text-xs">
            {backtestError}
          </div>
        )}

        <button
          onClick={() => backtestMutation.mutate()}
          disabled={!assetId || backtestMutation.isPending}
          className="px-5 py-2.5 rounded-xl bg-ocean-600 hover:bg-ocean-500 text-white text-xs font-semibold shadow-lg shadow-ocean-600/30 disabled:opacity-50 transition-all cursor-pointer"
        >
          {backtestMutation.isPending ? "Simulation Monte Carlo en cours…" : t("strategies.runBacktest")}
        </button>
      </div>

      {/* Backtest Results & Chart */}
      {result && (
        <div className="space-y-6 animate-fadeIn">
          {/* Metrics KPIs */}
          <div className="grid grid-cols-2 md:grid-cols-6 gap-3">
            <MetricCard label="Rendement Total" value={pct(result.totalReturn)} highlight={Number(result.totalReturn) >= 0} />
            <MetricCard label="Annualisé" value={pct(result.annualizedReturn)} />
            <MetricCard label="Max Drawdown" value={pct(result.maxDrawdown)} danger />
            <MetricCard label="Ratio Sharpe" value={Number(result.sharpeRatio).toFixed(2)} />
            <MetricCard label="Ratio Sortino" value={Number(result.sortinoRatio).toFixed(2)} />
            <MetricCard label="Taux de réussite" value={pct(result.winRate)} />
          </div>

          {/* Equity Curve with Monte Carlo */}
          <div className="rounded-2xl bg-[#0d1933] border border-ocean-500/20 p-6 shadow-xl space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <h3 className="text-sm font-semibold text-white">
                  {lang === "fr"
                    ? "Courbe d'équité : Stratégie vs Buy & Hold avec bandes de confiance Monte Carlo (P5 - P95)"
                    : "Equity curve: Strategy vs Buy & Hold with Monte Carlo Confidence Bands (P5 - P95)"}
                </h3>
                <p className="text-[11px] text-slate-400 mt-0.5">
                  {lang === "fr"
                    ? "Bandes probabilistes calculées sur 100 itérations de rééchantillonnage (Apache Commons Math MersenneTwister)."
                    : "Probabilistic bands computed over 100 bootstrap simulation paths."}
                </p>
              </div>
            </div>

            <div style={{ height: 380 }}>
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={chartData}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#132347" />
                  <XAxis dataKey="tick" stroke="#64748b" fontSize={11} />
                  <YAxis stroke="#64748b" fontSize={11} domain={["auto", "auto"]} />
                  <Tooltip
                    formatter={(val: any) => [formatMoney(val), "Valeur"]}
                    contentStyle={{ background: "#0d1933", border: "1px solid rgba(56,189,248,0.2)", borderRadius: "12px", fontSize: 11 }}
                  />
                  <Legend />
                  <Line type="monotone" dataKey="p95" stroke="#10B981" dot={false} name="Monte Carlo P95 (Optimiste)" strokeDasharray="3 3" />
                  <Line type="monotone" dataKey="p50" stroke="#F59E0B" dot={false} name="Monte Carlo P50 (Médiane)" strokeDasharray="3 3" />
                  <Line type="monotone" dataKey="p5" stroke="#EF4444" dot={false} name="Monte Carlo P5 (Défavorable)" strokeDasharray="3 3" />
                  <Line type="monotone" dataKey="buyAndHold" stroke="#94A3B8" dot={false} name="Buy & Hold (Référence)" />
                  <Line type="monotone" dataKey="strategy" stroke="#0284C7" strokeWidth={2.5} dot={false} name="Stratégie MyWallet" />
                </LineChart>
              </ResponsiveContainer>
            </div>
          </div>
        </div>
      )}

      <UserGuideModal isOpen={isGuideOpen} onClose={() => setIsGuideOpen(false)} />
    </div>
  );
}

function MetricCard({ label, value, highlight, danger }: { label: string; value: string; highlight?: boolean; danger?: boolean }) {
  return (
    <div className="rounded-xl border border-ocean-500/20 bg-[#0d1933] p-3.5 shadow-sm">
      <p className="text-[10px] text-slate-400 truncate">{label}</p>
      <p className={`text-base font-bold font-mono mt-1 ${highlight ? "text-emerald-400" : danger ? "text-rose-400" : "text-ocean-300"}`}>
        {value}
      </p>
    </div>
  );
}

function pct(value: string): string {
  return `${(Number(value) * 100).toFixed(2)}%`;
}

function daysAgoIso(days: number): string {
  const d = new Date();
  d.setDate(d.getDate() - days);
  return d.toISOString().slice(0, 10);
}
