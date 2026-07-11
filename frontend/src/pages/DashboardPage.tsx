import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { portfoliosApi } from "../api/endpoints";
import { useI18nStore } from "../i18n/useI18n";
import { useCurrencyStore } from "../store/useCurrencyStore";
import UserGuideModal from "../components/UserGuideModal";
import LiveMarketTickerTape from "../components/LiveMarketTickerTape";
import DynamicMarketChart from "../components/DynamicMarketChart";
import PortfolioAllocationChart from "../components/PortfolioAllocationChart";

export default function DashboardPage() {
  const { lang, t } = useI18nStore();
  const { formatMoney, currency } = useCurrencyStore();
  const [isGuideOpen, setIsGuideOpen] = useState(false);

  const { data: portfolios, isLoading, isError } = useQuery({
    queryKey: ["portfolios"],
    queryFn: portfoliosApi.list,
  });

  const totalValue = portfolios?.reduce((sum, p) => sum + Number(p.cashBalance), 0) ?? 0;

  return (
    <div className="p-4 md:p-8 max-w-7xl mx-auto space-y-6 md:space-y-8 animate-fadeIn">
      {/* Header section */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 pb-6 border-b border-ocean-500/20">
        <div>
          <h1 className="text-2xl font-bold text-white tracking-tight">
            {t("dashboard.title")}
          </h1>
          <p className="text-xs text-slate-400 mt-1">
            {t("dashboard.subtitle")}
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2.5">
          <button
            onClick={() => setIsGuideOpen(true)}
            className="flex items-center gap-2 px-3.5 py-2 rounded-xl bg-navy-850 border border-ocean-500/20 hover:border-ocean-500/40 text-ocean-300 text-xs font-medium transition-all shadow-sm cursor-pointer"
          >
            <span>📖</span>
            <span>{lang === "fr" ? "Manuel d'utilisation" : "User Manual"}</span>
          </button>

          <Link
            to="/portfolios"
            className="flex items-center gap-2 px-4 py-2 rounded-xl bg-ocean-600 hover:bg-ocean-500 text-white text-xs font-semibold shadow-md shadow-sky-950/40 transition-all cursor-pointer"
          >
            <span>+</span>
            <span>{t("portfolios.new")}</span>
          </Link>
        </div>
      </div>

      {/* 1. Live Market Tickers Tape (Mini Sparklines & WebSocket Ticks) */}
      <section className="space-y-2">
        <div className="flex items-center justify-between">
          <h2 className="text-xs font-semibold text-slate-400 uppercase tracking-wider flex items-center gap-1.5">
            <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
            <span>{lang === "fr" ? "Flux de Marché en Temps Réel" : "Real-Time Market Stream"}</span>
          </h2>
          <span className="text-[10px] text-ocean-300 font-mono">
            {lang === "fr" ? `Converti en ${currency}` : `Shown in ${currency}`}
          </span>
        </div>
        <LiveMarketTickerTape />
      </section>

      {/* 2. Top KPI Metrics Row */}
      {portfolios && (
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-4 md:gap-5">
          {/* Stat 1: Total Cash in selected currency */}
          <div className="p-5 rounded-2xl bg-[#0D1933] border border-ocean-500/20 shadow-lg hover:border-ocean-400/40 transition-all">
            <div className="flex items-center justify-between text-xs text-slate-400 mb-2">
              <span>{t("dashboard.totalCash")}</span>
              <span className="p-1.5 rounded-lg bg-ocean-500/10 text-ocean-400 text-sm">💰</span>
            </div>
            <p className="text-2xl font-bold font-mono text-white tracking-tight">
              {formatMoney(totalValue)}
            </p>
            <div className="flex items-center gap-1.5 mt-3 text-[11px] text-emerald-400 font-medium">
              <span>●</span>
              <span>{lang === "fr" ? `Converti en ${currency} (disponible)` : `Shown in ${currency} (liquid)`}</span>
            </div>
          </div>

          {/* Stat 2: Active Portfolios */}
          <div className="p-5 rounded-2xl bg-[#0D1933] border border-ocean-500/20 shadow-lg hover:border-ocean-400/40 transition-all">
            <div className="flex items-center justify-between text-xs text-slate-400 mb-2">
              <span>{t("dashboard.portfolioCount")}</span>
              <span className="p-1.5 rounded-lg bg-ocean-500/10 text-ocean-400 text-sm">💼</span>
            </div>
            <p className="text-2xl font-bold font-mono text-white tracking-tight">
              {portfolios.length}
            </p>
            <div className="flex items-center gap-1.5 mt-3 text-[11px] text-slate-400">
              <span>{lang === "fr" ? "Comptes simulés actifs" : "Active simulated accounts"}</span>
            </div>
          </div>

          {/* Stat 3: Execution Mode */}
          <div className="p-5 rounded-2xl bg-[#0D1933] border border-ocean-500/20 shadow-lg hover:border-ocean-400/40 transition-all sm:col-span-2 md:col-span-1">
            <div className="flex items-center justify-between text-xs text-slate-400 mb-2">
              <span>{t("dashboard.simulationMode")}</span>
              <span className="p-1.5 rounded-lg bg-emerald-500/10 text-emerald-400 text-sm">🛡️</span>
            </div>
            <p className="text-lg font-bold text-emerald-400">
              {t("dashboard.simulationModeDesc")}
            </p>
            <div className="flex items-center gap-1.5 mt-3 text-[11px] text-slate-400">
              <span>Event Sourcing + RabbitMQ</span>
            </div>
          </div>
        </div>
      )}

      {/* 3. Major Dynamic Interactive Trading Workstation Chart */}
      <section className="space-y-3">
        <div className="flex items-center justify-between">
          <h2 className="text-xs font-semibold text-slate-400 uppercase tracking-wider flex items-center gap-2">
            <span>📈</span>
            <span>{lang === "fr" ? "Terminal Graphique Dynamique & Indicateurs" : "Dynamic Charting Terminal & Indicators"}</span>
          </h2>
          <span className="text-[11px] font-mono text-slate-400">
            {lang === "fr" ? "Moyenne Mobile SMA(7) · Détection Z-Score" : "SMA(7) Moving Average · Z-Score Alerts"}
          </span>
        </div>
        <DynamicMarketChart initialSymbol="BTCUSDT" />
      </section>

      {/* Loading / Error States */}
      {isLoading && (
        <div className="p-12 text-center text-xs text-slate-400">
          <div className="w-8 h-8 mx-auto mb-3 border-2 border-ocean-400 border-t-transparent rounded-full animate-spin" />
          {lang === "fr" ? "Chargement des portefeuilles…" : "Loading portfolios…"}
        </div>
      )}

      {isError && (
        <div className="p-4 rounded-xl bg-rose-950/40 border border-rose-500/30 text-rose-300 text-xs">
          {lang === "fr" ? "Impossible de charger vos portefeuilles." : "Could not load your portfolios."}
        </div>
      )}

      {portfolios && (
        <>
          {/* 4. Portfolio Allocation Donut Chart & Portfolios Cards */}
          <section className="space-y-4">
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              {/* Left: Interactive Portfolio Allocation Donut */}
              <PortfolioAllocationChart portfolios={portfolios} />

              {/* Right: Quick Guide Banner & Platform Tips */}
              <div className="p-6 rounded-2xl bg-[#0D1933] border border-ocean-500/20 shadow-xl flex flex-col justify-between space-y-4">
                <div className="space-y-3">
                  <div className="flex items-center gap-2.5">
                    <span className="p-2 rounded-xl bg-ocean-600/20 text-ocean-400 text-lg">💡</span>
                    <div>
                      <h3 className="text-sm font-semibold text-white">
                        {lang === "fr" ? "Architecture du Système Financier" : "Financial Engine Architecture"}
                      </h3>
                      <p className="text-[11px] text-slate-400">
                        {lang === "fr" ? "Double contrôle de solvabilité & Traçabilité CQRS" : "Double solvency check & CQRS Auditability"}
                      </p>
                    </div>
                  </div>

                  <p className="text-xs text-slate-300 leading-relaxed">
                    {lang === "fr"
                      ? "Chaque ordre de trading est immuable. Les cours de marché sont générés par le moteur stochastique (Apache Commons Math & ta4j) et diffusés en continu via WebSockets STOMP."
                      : "Every trade is immutable. Market ticks are computed via stochastic processes and pushed through STOMP WebSockets."}
                  </p>

                  <div className="grid grid-cols-2 gap-2 pt-2 text-[11px] font-mono text-slate-400">
                    <div className="p-2 rounded-lg bg-[#132347] border border-ocean-500/10">
                      <span className="text-emerald-400 block font-bold">100% Audit</span>
                      <span>Event Sourced</span>
                    </div>
                    <div className="p-2 rounded-lg bg-[#132347] border border-ocean-500/10">
                      <span className="text-ocean-300 block font-bold">Z-Score 4.0</span>
                      <span>Anomalies Live</span>
                    </div>
                  </div>
                </div>

                <button
                  onClick={() => setIsGuideOpen(true)}
                  className="w-full py-2.5 rounded-xl bg-ocean-600 hover:bg-ocean-500 text-white text-xs font-semibold shadow-md shadow-sky-950/40 transition-all cursor-pointer"
                >
                  {lang === "fr" ? "Consulter le guide complet & AST" : "View full manual & AST rules"}
                </button>
              </div>
            </div>
          </section>

          {/* 5. Portfolios Cards Grid */}
          <section className="space-y-4">
            <div className="flex items-center justify-between">
              <h2 className="text-sm font-semibold text-slate-200">
                {t("dashboard.myPortfolios")}
              </h2>
              <span className="text-xs text-slate-400 font-mono">
                {portfolios.length} {lang === "fr" ? "enregistré(s)" : "found"}
              </span>
            </div>

            {portfolios.length === 0 ? (
              <div className="p-10 rounded-2xl border border-dashed border-ocean-500/20 bg-[#0D1933] text-center text-xs text-slate-400 space-y-3">
                <p>{t("dashboard.emptyPortfolios")}</p>
                <Link
                  to="/portfolios"
                  className="inline-block px-4 py-2 rounded-xl bg-ocean-600 hover:bg-ocean-500 text-white font-medium"
                >
                  {t("portfolios.new")}
                </Link>
              </div>
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 md:gap-5">
                {portfolios.map((p) => (
                  <Link
                    key={p.id}
                    to={`/portfolios/${p.id}`}
                    className="p-5 rounded-2xl bg-[#0D1933] border border-ocean-500/20 hover:border-ocean-400/50 hover:bg-[#132347] transition-all group shadow-md"
                  >
                    <div className="flex justify-between items-start mb-3">
                      <div>
                        <h3 className="font-semibold text-white group-hover:text-ocean-300 transition-colors text-sm">
                          {p.name}
                        </h3>
                        <p className="text-[10px] text-slate-400 font-mono mt-0.5">
                          ID: {p.id.slice(0, 8)}…
                        </p>
                      </div>
                      <span className="text-[10px] font-mono rounded-full bg-ocean-500/10 text-ocean-400 border border-ocean-500/20 px-2.5 py-0.5">
                        {p.mode}
                      </span>
                    </div>

                    <div className="pt-3 border-t border-ocean-500/10">
                      <p className="text-[11px] text-slate-400 mb-1">{t("dashboard.availableCash")}</p>
                      <p className="text-xl font-bold font-mono text-ocean-300">
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
          </section>
        </>
      )}

      <UserGuideModal isOpen={isGuideOpen} onClose={() => setIsGuideOpen(false)} />
    </div>
  );
}
