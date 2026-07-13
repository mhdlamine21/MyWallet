import { useEffect, useState } from "react";
import { AreaChart, Area, XAxis, YAxis, Tooltip, ResponsiveContainer } from "recharts";
import { useLivePricesMap } from "../hooks/useLivePricesMap";
import { useCurrencyStore } from "../store/useCurrencyStore";
import type { Asset, Position } from "../api/types";

const MAX_POINTS = 120;

export default function LiveEquityCurve({
  cashBalance,
  positions,
  assets,
}: {
  cashBalance: number;
  positions: Position[];
  assets: Asset[];
}) {
  const assetSymbolById = new Map(assets.map((a) => [a.id, a.symbol]));
  const symbols = [...new Set(positions.map((p) => assetSymbolById.get(p.assetId)).filter((s): s is string => !!s))];
  const livePrices = useLivePricesMap(symbols);

  const [points, setPoints] = useState<{ t: number; value: number; time: string }[]>([]);

  const currentValue = positions.reduce((sum, p) => {
    const symbol = assetSymbolById.get(p.assetId);
    const livePrice = symbol ? livePrices[symbol] : undefined;
    const price = livePrice ?? Number(p.averageAcquisitionPrice);
    return sum + Number(p.quantity) * price;
  }, cashBalance);

  useEffect(() => {
    const time = new Date().toLocaleTimeString();
    setPoints((prev) => {
      const next = [...prev, { t: prev.length, value: currentValue, time }];
      return next.length > MAX_POINTS ? next.slice(next.length - MAX_POINTS) : next;
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentValue]);

  const { formatMoney } = useCurrencyStore();

  return (
    <div className="rounded-2xl border border-ocean-500/20 bg-[#0d1933] p-5 shadow-lg">
      <div className="flex items-center justify-between mb-3 border-b border-ocean-500/10 pb-3">
        <div className="flex items-center gap-2">
          <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
          <span className="text-xs font-semibold text-slate-300">Courbe de Valeur Totale en Direct</span>
        </div>
        <span className="text-xl font-bold font-mono text-ocean-300">{formatMoney(currentValue)}</span>
      </div>

      <div style={{ height: 160 }}>
        {points.length > 1 ? (
          <ResponsiveContainer width="100%" height="100%">
            <AreaChart data={points}>
              <defs>
                <linearGradient id="equityGrad" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#0284c7" stopOpacity={0.45} />
                  <stop offset="95%" stopColor="#0284c7" stopOpacity={0.0} />
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
                formatter={(value: any) => [formatMoney(Number(value) || 0), "Valorisation"]}
              />
              <Area
                type="monotone"
                dataKey="value"
                stroke="#38BDF8"
                strokeWidth={2.5}
                fillOpacity={1}
                fill="url(#equityGrad)"
                isAnimationActive={false}
              />
            </AreaChart>
          </ResponsiveContainer>
        ) : (
          <p className="text-xs text-slate-500 flex items-center justify-center h-full">Accumulation des données en direct…</p>
        )}
      </div>
    </div>
  );
}
