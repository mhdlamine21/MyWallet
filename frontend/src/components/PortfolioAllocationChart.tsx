import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip } from "recharts";
import { useCurrencyStore } from "../store/useCurrencyStore";
import type { Portfolio } from "../api/types";

interface PortfolioAllocationChartProps {
  portfolios: Portfolio[];
}

const PALETTE = ["#0284C7", "#38BDF8", "#10B981", "#F59E0B", "#6366F1", "#EC4899"];

export default function PortfolioAllocationChart({ portfolios }: PortfolioAllocationChartProps) {
  const { formatMoney } = useCurrencyStore();

  const data = portfolios.map((p, idx) => ({
    name: p.name,
    value: Number(p.cashBalance) || 0,
    color: PALETTE[idx % PALETTE.length],
    mode: p.mode,
  }));

  const totalValue = data.reduce((sum, item) => sum + item.value, 0);

  if (data.length === 0 || totalValue === 0) {
    return (
      <div className="rounded-2xl bg-[#0d1933] border border-ocean-500/20 p-6 flex items-center justify-center text-xs text-slate-500 min-h-[260px]">
        Aucune allocation à afficher pour le moment.
      </div>
    );
  }

  return (
    <div className="w-full min-w-0 rounded-2xl bg-[#0d1933] border border-ocean-500/20 shadow-xl p-4 sm:p-5 md:p-6 space-y-4 overflow-hidden">
      <div className="flex items-center justify-between border-b border-ocean-500/20 pb-3">
        <h3 className="text-sm font-semibold text-white flex items-center gap-2">
          <span>📊</span>
          <span>Répartition du Capital & Portefeuilles</span>
        </h3>
        <span className="text-[11px] font-mono text-ocean-300 font-bold">
          Total : {formatMoney(totalValue)}
        </span>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4 items-center">
        {/* Donut chart */}
        <div className="w-full min-w-0 h-[210px] flex items-center justify-center">
          <ResponsiveContainer width="100%" height="100%">
            <PieChart>
              <Pie
                data={data}
                cx="50%"
                cy="50%"
                innerRadius={55}
                outerRadius={80}
                paddingAngle={4}
                dataKey="value"
              >
                {data.map((entry, index) => (
                  <Cell key={`cell-${index}`} fill={entry.color} stroke="#0d1933" strokeWidth={2} />
                ))}
              </Pie>
              <Tooltip
                contentStyle={{
                  backgroundColor: "#0d1933",
                  borderColor: "rgba(56, 189, 248, 0.3)",
                  borderRadius: "12px",
                  fontSize: 12,
                }}
                formatter={(val: any) => [formatMoney(Number(val)), "Solde"]}
              />
            </PieChart>
          </ResponsiveContainer>
        </div>

        {/* Legend with percentages */}
        <div className="space-y-2.5 max-h-52 overflow-y-auto pr-1">
          {data.map((item) => {
            const pct = totalValue > 0 ? ((item.value / totalValue) * 100).toFixed(1) : "0.0";
            return (
              <div
                key={item.name}
                className="flex items-center justify-between p-2.5 rounded-xl bg-[#132347] border border-ocean-500/20 text-xs"
              >
                <div className="flex items-center gap-2 overflow-hidden">
                  <span className="w-3 h-3 rounded-md shrink-0" style={{ backgroundColor: item.color }} />
                  <div className="truncate">
                    <span className="font-semibold text-white block truncate">{item.name}</span>
                    <span className="text-[10px] text-slate-400 font-mono">{item.mode}</span>
                  </div>
                </div>

                <div className="text-right shrink-0">
                  <div className="font-mono font-bold text-ocean-300">{formatMoney(item.value)}</div>
                  <div className="text-[10px] text-slate-400 font-mono">{pct}%</div>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
