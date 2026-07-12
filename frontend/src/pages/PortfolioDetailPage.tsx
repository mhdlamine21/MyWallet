import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useParams, Link } from "react-router-dom";
import { ordersApi, portfoliosApi, riskApi, assetsApi } from "../api/endpoints";
import type { ApiError } from "../api/types";
import { isAxiosError } from "axios";
import { useStompTopic } from "../hooks/useStompTopic";
import LivePriceChart from "../components/LivePriceChart";
import LiveEquityCurve from "../components/LiveEquityCurve";
import { useI18nStore } from "../i18n/useI18n";
import { useCurrencyStore } from "../store/useCurrencyStore";
import UserGuideModal from "../components/UserGuideModal";

export default function PortfolioDetailPage() {
  const { id } = useParams<{ id: string }>();
  const portfolioId = id!;
  const queryClient = useQueryClient();
  const { lang, t } = useI18nStore();
  const { formatMoney } = useCurrencyStore();
  const [isGuideOpen, setIsGuideOpen] = useState(false);

  const { data: portfolio } = useQuery({ queryKey: ["portfolio", portfolioId], queryFn: () => portfoliosApi.get(portfolioId) });
  const { data: positions } = useQuery({ queryKey: ["positions", portfolioId], queryFn: () => portfoliosApi.positions(portfolioId) });
  const { data: orders } = useQuery({ queryKey: ["orders", portfolioId], queryFn: () => ordersApi.list(portfolioId) });
  const { data: alerts } = useQuery({ queryKey: ["risk-alerts", portfolioId], queryFn: () => riskApi.alerts(portfolioId) });
  const { data: assets } = useQuery({ queryKey: ["assets"], queryFn: assetsApi.list });

  const { connected: liveAlertsConnected } = useStompTopic(
    `/topic/portfolios/${portfolioId}/risk-alerts`,
    () => queryClient.invalidateQueries({ queryKey: ["risk-alerts", portfolioId] })
  );
  useStompTopic(`/topic/portfolios/${portfolioId}/orders`, () => {
    queryClient.invalidateQueries({ queryKey: ["orders", portfolioId] });
    queryClient.invalidateQueries({ queryKey: ["positions", portfolioId] });
    queryClient.invalidateQueries({ queryKey: ["portfolio", portfolioId] });
  });

  const [assetId, setAssetId] = useState("");
  const [side, setSide] = useState("BUY");
  const [orderType, setOrderType] = useState("LIMIT");
  const [quantity, setQuantity] = useState("");
  const [limitPrice, setLimitPrice] = useState("");
  const [orderError, setOrderError] = useState<string | null>(null);
  const [orderSuccess, setOrderSuccess] = useState<string | null>(null);

  const createOrderMutation = useMutation({
    mutationFn: () =>
      ordersApi.create({
        portfolioId,
        assetId,
        orderType,
        side,
        quantity,
        ...(orderType !== "MARKET" ? { limitPrice } : {}),
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["orders", portfolioId] });
      setOrderError(null);
      setOrderSuccess(t("portfolioDetail.orderSuccess"));
      setQuantity("");
      setTimeout(() => setOrderSuccess(null), 5000);
    },
    onError: (err) => {
      setOrderSuccess(null);
      if (isAxiosError<ApiError>(err) && err.response) {
        setOrderError(err.response.data.message);
      } else {
        setOrderError(lang === "fr" ? "Impossible d'exécuter l'ordre." : "Could not place the order.");
      }
    },
  });

  return (
    <div className="p-4 md:p-8 max-w-7xl mx-auto space-y-6 md:space-y-8 animate-fadeIn">
      {/* Breadcrumb & Navigation */}
      <div className="flex items-center justify-between pb-4 border-b border-ocean-500/20">
        <div className="flex items-center gap-3">
          <Link to="/portfolios" className="text-xs text-slate-400 hover:text-ocean-300 transition-colors">
            &larr; {t("nav.portfolios")}
          </Link>
          <span className="text-slate-600">/</span>
          <span className="text-xs text-white font-medium">{portfolio?.name ?? "..."}</span>
        </div>

        <button
          onClick={() => setIsGuideOpen(true)}
          className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-navy-850 border border-ocean-500/30 text-ocean-300 text-xs font-medium hover:border-ocean-500/60"
        >
          <span>📖</span>
          <span>{lang === "fr" ? "Manuel des Ordres & Risque" : "Orders & Risk Guide"}</span>
        </button>
      </div>

      {/* Main Portfolio Header */}
      <div className="p-6 rounded-2xl bg-[#0d1933] border border-ocean-500/20 shadow-xl flex flex-col md:flex-row md:items-center justify-between gap-6 relative overflow-hidden">
        <div className="absolute top-0 right-0 w-64 h-64 bg-ocean-600/10 rounded-full blur-3xl pointer-events-none" />

        <div>
          <div className="flex items-center gap-3 mb-1">
            <h1 className="text-2xl font-bold text-white tracking-tight">{portfolio?.name}</h1>
            <span
              className={`inline-flex items-center gap-1.5 text-[11px] font-mono px-2.5 py-0.5 rounded-full border ${
                liveAlertsConnected
                  ? "bg-emerald-950/50 text-emerald-400 border-emerald-500/30"
                  : "bg-navy-850 text-slate-400 border-slate-700"
              }`}
            >
              <span className={`w-1.5 h-1.5 rounded-full ${liveAlertsConnected ? "bg-emerald-400 animate-pulse" : "bg-slate-500"}`} />
              {liveAlertsConnected ? t("portfolioDetail.liveUpdates") : t("portfolioDetail.liveDisconnected")}
            </span>
          </div>
          <p className="text-xs text-slate-400 font-mono">
            ID: {portfolioId} · Mode: {portfolio?.mode}
          </p>
        </div>

        <div className="text-left md:text-right">
          <p className="text-xs text-slate-400 mb-1">{t("dashboard.totalCash")}</p>
          <p className="text-3xl font-bold font-mono text-ocean-300">
            {formatMoney(portfolio?.cashBalance ?? 0)}
          </p>
        </div>
      </div>

      {/* Live Equity Curve Section */}
      {portfolio && positions && assets && (
        <div className="rounded-2xl bg-[#0d1933] border border-ocean-500/20 p-6 shadow-xl">
          <h2 className="text-sm font-semibold text-white mb-4 flex items-center gap-2">
            <span>📈</span>
            <span>{t("portfolioDetail.equityCurve")}</span>
          </h2>
          <LiveEquityCurve cashBalance={Number(portfolio.cashBalance)} positions={positions} assets={assets} />
        </div>
      )}

      {/* Grid: Trading Terminal & Positions */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left 2 Cols: Positions & Market Tickers */}
        <div className="lg:col-span-2 space-y-6">
          {/* Held Positions Card */}
          <div className="rounded-2xl bg-[#0d1933] border border-ocean-500/20 p-6 shadow-xl">
            <h2 className="text-sm font-semibold text-white mb-4 flex items-center gap-2">
              <span>💼</span>
              <span>{t("portfolioDetail.positions")}</span>
            </h2>

            {positions && positions.length === 0 && (
              <div className="p-8 text-center text-xs text-slate-500 border border-dashed border-ocean-500/20 rounded-xl">
                {t("portfolioDetail.noPositions")}
              </div>
            )}

            {positions && positions.length > 0 && (
              <div className="overflow-x-auto">
                <table className="w-full text-xs">
                  <thead className="text-slate-400 text-left border-b border-ocean-500/20">
                    <tr>
                      <th className="py-2.5 px-3 font-semibold">{t("portfolioDetail.asset")}</th>
                      <th className="py-2.5 px-3 font-semibold">{t("portfolioDetail.quantity")}</th>
                      <th className="py-2.5 px-3 font-semibold">{t("portfolioDetail.avgPrice")}</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-ocean-500/10">
                    {positions.map((p) => {
                      const asset = assets?.find((a) => a.id === p.assetId);
                      return (
                        <tr key={p.id} className="hover:bg-white/[0.02] transition-colors">
                          <td className="py-3 px-3 font-medium text-white flex items-center gap-2">
                            <span className="w-6 h-6 rounded-lg bg-ocean-500/20 text-ocean-300 flex items-center justify-center font-mono text-[10px] font-bold">
                              {asset?.symbol.slice(0, 3) ?? "AST"}
                            </span>
                            <div>
                              <div>{asset?.symbol ?? p.assetId}</div>
                              <div className="text-[10px] text-slate-400">{asset?.displayName ?? "Simulated Asset"}</div>
                            </div>
                          </td>
                          <td className="py-3 px-3 font-mono font-medium text-slate-200">{p.quantity}</td>
                          <td className="py-3 px-3 font-mono text-ocean-300">{formatMoney(p.averageAcquisitionPrice)}</td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </div>

          {/* Live Market Price Tickers */}
          {positions && positions.length > 0 && assets && (
            <div>
              <h2 className="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-3">
                {lang === "fr" ? "Flux de marché en direct (STOMP WebSocket)" : "Live Market Tickers (STOMP WebSocket)"}
              </h2>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {[...new Set(positions.map((p) => assets.find((a) => a.id === p.assetId)?.symbol).filter((s): s is string => !!s))]
                  .map((symbol) => (
                    <LivePriceChart key={symbol} symbol={symbol} />
                  ))}
              </div>
            </div>
          )}
        </div>

        {/* Right Col: Order Placement Terminal */}
        <div className="space-y-6">
          <div className="rounded-2xl bg-[#0d1933] border border-ocean-500/20 p-6 shadow-xl relative overflow-hidden">
            <h2 className="text-sm font-semibold text-white mb-4 flex items-center gap-2">
              <span>⚡</span>
              <span>{t("portfolioDetail.placeOrder")}</span>
            </h2>

            {/* Buy / Sell Tabs */}
            <div className="grid grid-cols-2 gap-1.5 p-1 rounded-xl bg-[#132347] border border-ocean-500/20 mb-4">
              <button
                type="button"
                onClick={() => setSide("BUY")}
                className={`py-2 text-xs font-bold rounded-lg transition-all ${
                  side === "BUY"
                    ? "bg-emerald-600 text-white shadow-md shadow-emerald-600/30"
                    : "text-slate-400 hover:text-white"
                }`}
              >
                {t("portfolioDetail.buy")}
              </button>
              <button
                type="button"
                onClick={() => setSide("SELL")}
                className={`py-2 text-xs font-bold rounded-lg transition-all ${
                  side === "SELL"
                    ? "bg-rose-600 text-white shadow-md shadow-rose-600/30"
                    : "text-slate-400 hover:text-white"
                }`}
              >
                {t("portfolioDetail.sell")}
              </button>
            </div>

            <div className="space-y-3.5">
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
                <label className="block text-[11px] font-medium text-slate-400 mb-1">{t("portfolioDetail.orderType")}</label>
                <div className="grid grid-cols-2 gap-2">
                  {["LIMIT", "MARKET"].map((type) => (
                    <button
                      key={type}
                      type="button"
                      onClick={() => setOrderType(type)}
                      className={`py-1.5 text-xs font-medium rounded-lg border transition-all ${
                        orderType === type
                          ? "bg-ocean-600/40 border-ocean-400 text-white"
                          : "bg-[#132347] border-ocean-500/20 text-slate-400 hover:text-white"
                      }`}
                    >
                      {type}
                    </button>
                  ))}
                </div>
              </div>

              <div>
                <label className="block text-[11px] font-medium text-slate-400 mb-1">{t("portfolioDetail.quantity")}</label>
                <input
                  type="number"
                  placeholder="0.0"
                  value={quantity}
                  onChange={(e) => setQuantity(e.target.value)}
                  className="w-full rounded-xl bg-[#132347] border border-ocean-500/20 focus:border-ocean-400 focus:ring-1 focus:ring-ocean-400 px-3 py-2 text-xs text-white outline-none font-mono"
                />
              </div>

              {orderType !== "MARKET" && (
                <div>
                  <label className="block text-[11px] font-medium text-slate-400 mb-1">{t("portfolioDetail.limitPrice")}</label>
                  <input
                    type="number"
                    placeholder="0.00"
                    value={limitPrice}
                    onChange={(e) => setLimitPrice(e.target.value)}
                    className="w-full rounded-xl bg-[#132347] border border-ocean-500/20 focus:border-ocean-400 focus:ring-1 focus:ring-ocean-400 px-3 py-2 text-xs text-white outline-none font-mono"
                  />
                </div>
              )}

              {orderError && (
                <div className="p-2.5 rounded-lg bg-red-950/40 border border-red-500/30 text-red-300 text-xs">
                  {orderError}
                </div>
              )}

              {orderSuccess && (
                <div className="p-2.5 rounded-lg bg-emerald-950/40 border border-emerald-500/30 text-emerald-300 text-xs">
                  {orderSuccess}
                </div>
              )}

              <button
                type="button"
                onClick={() => createOrderMutation.mutate()}
                disabled={!assetId || !quantity || createOrderMutation.isPending}
                className={`w-full py-2.5 rounded-xl font-bold text-xs text-white shadow-lg transition-all disabled:opacity-50 cursor-pointer ${
                  side === "BUY"
                    ? "bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-500 hover:to-teal-500 shadow-emerald-600/30"
                    : "bg-gradient-to-r from-rose-600 to-red-600 hover:from-rose-500 hover:to-red-500 shadow-rose-600/30"
                }`}
              >
                {createOrderMutation.isPending
                  ? "Validation en cours…"
                  : `${side === "BUY" ? "Acheter" : "Vendre"} ${assets?.find((a) => a.id === assetId)?.symbol ?? ""}`}
              </button>
            </div>
          </div>

          {/* Risk Alerts Box */}
          <div className="rounded-2xl bg-[#0d1933] border border-ocean-500/20 p-5 shadow-xl">
            <h3 className="text-xs font-semibold text-slate-300 mb-3 flex items-center justify-between">
              <span className="flex items-center gap-1.5">
                <span>🛡️</span>
                <span>{t("portfolioDetail.riskAlerts")}</span>
              </span>
              <span className="text-[10px] font-mono text-ocean-400">{alerts?.length ?? 0}</span>
            </h3>

            {(!alerts || alerts.length === 0) && (
              <p className="text-[11px] text-slate-500">{t("portfolioDetail.noAlerts")}</p>
            )}

            {alerts && alerts.length > 0 && (
              <div className="space-y-2 max-h-48 overflow-y-auto">
                {alerts.map((al) => (
                  <div key={al.id} className="p-2.5 rounded-xl bg-amber-950/30 border border-amber-500/20 text-xs text-amber-300">
                    <div className="font-semibold text-[11px]">{al.limitType}</div>
                    <div className="text-[10px] text-amber-200/80 mt-0.5">{al.explanation}</div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Order History */}
      <div className="rounded-2xl bg-[#0d1933] border border-ocean-500/20 p-6 shadow-xl">
        <h2 className="text-sm font-semibold text-white mb-4 flex items-center gap-2">
          <span>📜</span>
          <span>{t("portfolioDetail.orderHistory")}</span>
        </h2>

        {orders && orders.length === 0 && (
          <p className="text-xs text-slate-500">{lang === "fr" ? "Aucun ordre passé sur ce portefeuille." : "No orders recorded yet."}</p>
        )}

        {orders && orders.length > 0 && (
          <div className="overflow-x-auto">
            <table className="w-full text-xs">
              <thead className="text-slate-400 text-left border-b border-ocean-500/20">
                <tr>
                  <th className="py-2.5 px-3">Date</th>
                  <th className="py-2.5 px-3">ID Ordre</th>
                  <th className="py-2.5 px-3">Actif</th>
                  <th className="py-2.5 px-3">Sens</th>
                  <th className="py-2.5 px-3">Type</th>
                  <th className="py-2.5 px-3">Quantité</th>
                  <th className="py-2.5 px-3">Statut</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-ocean-500/10">
                {orders.map((o) => (
                  <tr key={o.id} className="hover:bg-white/[0.02]">
                    <td className="py-2.5 px-3 text-slate-400 font-mono text-[11px]">{new Date(o.createdAt).toLocaleTimeString()}</td>
                    <td className="py-2.5 px-3 font-mono text-slate-500 text-[10px]">{o.id.slice(0, 8)}…</td>
                    <td className="py-2.5 px-3 font-medium text-white">{assets?.find((a) => a.id === o.assetId)?.symbol ?? o.assetId}</td>
                    <td className="py-2.5 px-3">
                      <span className={`px-2 py-0.5 rounded font-bold text-[10px] ${o.side === "BUY" ? "bg-emerald-950/60 text-emerald-400 border border-emerald-500/20" : "bg-rose-950/60 text-rose-400 border border-rose-500/20"}`}>
                        {o.side}
                      </span>
                    </td>
                    <td className="py-2.5 px-3 text-slate-400">{o.orderType}</td>
                    <td className="py-2.5 px-3 font-mono text-slate-200">{o.quantity}</td>
                    <td className="py-2.5 px-3">
                      <span className={`px-2 py-0.5 rounded font-mono text-[10px] ${o.status === "EXECUTED" ? "bg-emerald-500/10 text-emerald-400 border border-emerald-500/20" : o.status === "REJECTED" ? "bg-red-500/10 text-red-400 border border-red-500/20" : "bg-ocean-500/10 text-ocean-300 border border-ocean-500/20"}`}>
                        {o.status}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <UserGuideModal isOpen={isGuideOpen} onClose={() => setIsGuideOpen(false)} />
    </div>
  );
}
