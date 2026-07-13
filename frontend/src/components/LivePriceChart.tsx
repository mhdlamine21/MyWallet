import { useState } from "react";
import { AreaChart, Area, XAxis, YAxis, Tooltip, ResponsiveContainer } from "recharts";
import { useStompTopic } from "../hooks/useStompTopic";
import { useCurrencyStore } from "../store/useCurrencyStore";

interface PriceTick {
  symbol: string;
  price: number;
  observedAt: string;
}

const MAX_POINTS = 60;

export default function LivePriceChart({ symbol }: { symbol: string }) {
  const [points, setPoints] = useState<{ t: number; price: number; time: string }[]>([]);
  const [latest, setLatest] = useState<number | null>(null);
  const [previous, setPrevious] = useState<number | null>(null);
  const [lastAnomaly, setLastAnomaly] = useState<{ zScore: number; explanation: string } | null>(null);

  const { formatMoney } = useCurrencyStore();

  const { connected } = useStompTopic<PriceTick>(`/topic/prices/${symbol}`, (tick) => {
    setPrevious((prev) => (latest !== null ? latest : prev));
    setLatest(tick.price);
    const time = new Date().toLocaleTimeString();
    setPoints((prev) => {
      const next = [...prev, { t: prev.length, price: tick.price, time }];
      return next.length > MAX_POINTS ? next.slice(next.length - MAX_POINTS) : next;
    });
  });

  useStompTopic<{ zScore: number; explanation: string }>(`/topic/anomalies/${symbol}`, (anomaly) => {
    setLastAnomaly(anomaly);
  });

  const direction = latest !== null && previous !== null ? (latest > previous ? "up" : latest < previous ? "down" : "flat") : "flat";
  const colorClass = direction === "up" ? "text-emerald-400" : direction === "down" ? "text-rose-400" : "text-slate-300";

  return (
    <div className="rounded-2xl border border-ocean-500/20 bg-[#0d1933] p-4 shadow-md hover:border-ocean-500/40 transition-all">
      <div className="flex items-center justify-between mb-3">
        <div className="flex items-center gap-2">
          <span className="text-xs font-bold text-white tracking-wide">{symbol}</span>
          <span className={`inline-block w-2 h-2 rounded-full ${connected ? "bg-emerald-400 animate-pulse" : "bg-slate-600"}`} />
          {lastAnomaly && (
            <span
              className="text-[9px] rounded-full bg-amber-950/60 text-amber-300 border border-amber-500/30 px-2 py-0.5"
              title={lastAnomaly.explanation}
            >
              Anomalie (z={lastAnomaly.zScore.toFixed(1)})
            </span>
          )}
        </div>
        <span className={`text-sm font-mono font-bold ${colorClass}`}>
          {latest !== null ? formatMoney(latest) : "Attente..."}
        </span>
      </div>

      <div style={{ height: 120 }}>
        {points.length > 1 ? (
          <ResponsiveContainer width="100%" height="100%">
            <AreaChart data={points}>
              <defs>
                <linearGradient id={`grad-${symbol}`} x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor={direction === "down" ? "#F43F5E" : "#0284C7"} stopOpacity={0.4} />
                  <stop offset="95%" stopColor={direction === "down" ? "#F43F5E" : "#0284C7"} stopOpacity={0.0} />
                </linearGradient>
              </defs>
              <XAxis dataKey="t" hide />
              <YAxis hide domain={["auto", "auto"]} />
              <Tooltip
                contentStyle={{
                  background: "#0d1933",
                  border: "1px solid rgba(56, 189, 248, 0.3)",
                  borderRadius: "10px",
                  fontSize: 11,
                }}
                labelFormatter={(_, payload) => payload?.[0]?.payload?.time ?? ""}
                formatter={(value: any) => [formatMoney(Number(value) || 0), "Cours"]}
              />
              <Area
                type="monotone"
                dataKey="price"
                stroke={direction === "down" ? "#F43F5E" : "#38BDF8"}
                strokeWidth={2}
                fillOpacity={1}
                fill={`url(#grad-${symbol})`}
                isAnimationActive={false}
              />
            </AreaChart>
          </ResponsiveContainer>
        ) : (
          <p className="text-xs text-slate-500 flex items-center justify-center h-full">En attente des ticks live…</p>
        )}
      </div>
    </div>
  );
}
