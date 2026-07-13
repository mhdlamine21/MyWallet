import { useState, useMemo } from "react";
import {
  AreaChart,
  Area,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  ReferenceLine,
} from "recharts";
import { useStompTopic } from "../hooks/useStompTopic";
import { useCurrencyStore } from "../store/useCurrencyStore";

interface DynamicMarketChartProps {
  initialSymbol?: string;
}

const ASSET_OPTIONS = [
  { symbol: "BTCUSDT", label: "Bitcoin", icon: "₿", base: 60000 },
  { symbol: "ETHUSDT", label: "Ethereum", icon: "Ξ", base: 3000 },
  { symbol: "AAPL", label: "Apple", icon: "🍎", base: 190 },
  { symbol: "EURUSD", label: "EUR/USD", icon: "💶", base: 1.08 },
];

interface TickPoint {
  index: number;
  time: string;
  price: number;
  sma?: number;
}

export default function DynamicMarketChart({ initialSymbol = "BTCUSDT" }: DynamicMarketChartProps) {
  const [activeSymbol, setActiveSymbol] = useState(initialSymbol);
  const [windowSize, setWindowSize] = useState<number>(60);
  const [showSMA, setShowSMA] = useState(true);
  const [history, setHistory] = useState<Record<string, TickPoint[]>>({
    BTCUSDT: generateInitialData(60000, 30),
    ETHUSDT: generateInitialData(3000, 30),
    AAPL: generateInitialData(190, 30),
    EURUSD: generateInitialData(1.08, 30),
  });
  const [lastAnomaly, setLastAnomaly] = useState<{ zScore: number; explanation: string } | null>(null);

  const { formatMoney } = useCurrencyStore();

  // Listen to live ticks for the active symbol
  const { connected } = useStompTopic<{ symbol: string; price: number; observedAt: string }>(
    `/topic/prices/${activeSymbol}`,
    (tick) => {
      setHistory((prev) => {
        const symbolList = prev[activeSymbol] || [];
        const timeStr = new Date().toLocaleTimeString();
        const newPoint: TickPoint = {
          index: symbolList.length,
          time: timeStr,
          price: tick.price,
        };

        const updated = [...symbolList, newPoint];
        const trimmed = updated.length > 200 ? updated.slice(updated.length - 200) : updated;

        // Calculate moving average
        const withSma = computeMovingAverage(trimmed, 7);
        return { ...prev, [activeSymbol]: withSma };
      });
    }
  );

  useStompTopic<{ zScore: number; explanation: string }>(`/topic/anomalies/${activeSymbol}`, (anomaly) => {
    setLastAnomaly(anomaly);
  });

  const currentPoints = useMemo(() => {
    const list = history[activeSymbol] || [];
    return list.slice(-windowSize);
  }, [history, activeSymbol, windowSize]);

  const latestPoint = currentPoints[currentPoints.length - 1];
  const firstPoint = currentPoints[0];
  const latestPrice = latestPoint?.price ?? 0;
  const initialPrice = firstPoint?.price ?? latestPrice;
  const priceDiff = latestPrice - initialPrice;
  const pctDiff = initialPrice !== 0 ? ((priceDiff / initialPrice) * 100).toFixed(2) : "0.00";
  const isPositive = priceDiff >= 0;

  // Min / Max calculation for dynamic YAxis and ReferenceLines
  const prices = currentPoints.map((p) => p.price);
  const minPrice = prices.length > 0 ? Math.min(...prices) : 0;
  const maxPrice = prices.length > 0 ? Math.max(...prices) : 0;
  const avgPrice = prices.length > 0 ? prices.reduce((a, b) => a + b, 0) / prices.length : 0;

  return (
    <div className="w-full min-w-0 rounded-2xl bg-[#0d1933] border border-ocean-500/20 shadow-xl p-4 sm:p-5 md:p-6 space-y-4 md:space-y-5 relative overflow-hidden">
      {/* Background radial glow */}
      <div className="absolute top-0 right-0 w-80 h-80 bg-ocean-600/10 rounded-full blur-3xl pointer-events-none" />

      {/* Top Controls: Asset Switcher + Window & Indicator controls */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-ocean-500/20 pb-4 relative z-10">
        {/* Asset tabs */}
        <div className="flex items-center gap-1.5 sm:gap-2 overflow-x-auto pb-1 sm:pb-0 scrollbar-none">
          {ASSET_OPTIONS.map((opt) => (
            <button
              key={opt.symbol}
              onClick={() => {
                setActiveSymbol(opt.symbol);
                setLastAnomaly(null);
              }}
              className={`flex items-center gap-1.5 px-2.5 sm:px-3 py-1.5 rounded-xl text-xs font-semibold transition-all shrink-0 cursor-pointer ${
                activeSymbol === opt.symbol
                  ? "bg-ocean-600 text-white shadow-md shadow-ocean-600/30 ring-1 ring-white/20"
                  : "bg-[#132347] border border-ocean-500/20 text-slate-300 hover:text-white"
              }`}
            >
              <span>{opt.icon}</span>
              <span>{opt.symbol}</span>
            </button>
          ))}
        </div>

        {/* Window size buttons & SMA toggle */}
        <div className="flex items-center gap-2 flex-wrap sm:justify-end">
          <div className="flex bg-[#132347] p-0.5 rounded-xl border border-ocean-500/20 text-[10px] sm:text-[11px]">
            {[
              { size: 30, label: "30t" },
              { size: 60, label: "60t" },
              { size: 120, label: "120t" },
            ].map((btn) => (
              <button
                key={btn.size}
                onClick={() => setWindowSize(btn.size)}
                className={`px-2 sm:px-2.5 py-1 rounded-lg font-medium transition-all cursor-pointer ${
                  windowSize === btn.size ? "bg-ocean-600 text-white shadow-sm" : "text-slate-400 hover:text-white"
                }`}
              >
                {btn.label}
              </button>
            ))}
          </div>

          <button
            onClick={() => setShowSMA(!showSMA)}
            className={`px-2.5 py-1 rounded-xl border text-[10px] sm:text-[11px] font-medium transition-all cursor-pointer flex items-center gap-1 shrink-0 ${
              showSMA
                ? "bg-amber-950/40 text-amber-300 border-amber-500/40"
                : "bg-[#132347] text-slate-400 border-ocean-500/20 hover:text-white"
            }`}
          >
            <span>📈</span>
            <span>SMA</span>
          </button>

          {/* WebSocket Pulse Badge */}
          <div className="flex items-center gap-1.5 px-2 py-1 rounded-xl bg-navy-850 border border-ocean-500/20 text-[10px] sm:text-[11px] font-mono text-slate-300 shrink-0">
            <span className={`w-1.5 h-1.5 rounded-full ${connected ? "bg-emerald-400 animate-pulse" : "bg-slate-500"}`} />
            <span>{connected ? "LIVE" : "OFF"}</span>
          </div>
        </div>
      </div>

      {/* Price Header & Dynamic Indicators Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 relative z-10">
        <div>
          <div className="flex items-center gap-2.5 flex-wrap">
            <h3 className="text-xl sm:text-2xl md:text-3xl font-bold font-mono text-white tracking-tight">
              {formatMoney(latestPrice)}
            </h3>
            <span
              className={`text-[11px] font-mono font-bold px-2 py-0.5 rounded-full border ${
                isPositive
                  ? "bg-emerald-950/60 text-emerald-400 border-emerald-500/30"
                  : "bg-rose-950/60 text-rose-400 border-rose-500/30"
              }`}
            >
              {isPositive ? `▲ +${pctDiff}%` : `▼ ${pctDiff}%`}
            </span>
          </div>
          <p className="text-[10px] sm:text-[11px] text-slate-400 font-mono mt-0.5">
            Dernier tick : {latestPoint?.time ?? "En attente"} · {currentPoints.length} points
          </p>
        </div>

        {/* Live Mini Stats Strip - 3 columns on mobile, clean wrapping */}
        <div className="grid grid-cols-3 gap-2 text-xs w-full sm:w-auto">
          <div className="p-2 rounded-xl bg-[#132347] border border-ocean-500/20 text-center sm:text-left">
            <span className="text-[9px] text-slate-400 block truncate">Plus Haut</span>
            <span className="font-mono font-bold text-emerald-400 text-[11px] sm:text-xs truncate block">
              {formatMoney(maxPrice)}
            </span>
          </div>
          <div className="p-2 rounded-xl bg-[#132347] border border-ocean-500/20 text-center sm:text-left">
            <span className="text-[9px] text-slate-400 block truncate">Plus Bas</span>
            <span className="font-mono font-bold text-rose-400 text-[11px] sm:text-xs truncate block">
              {formatMoney(minPrice)}
            </span>
          </div>
          <div className="p-2 rounded-xl bg-[#132347] border border-ocean-500/20 text-center sm:text-left">
            <span className="text-[9px] text-slate-400 block truncate">Moyenne</span>
            <span className="font-mono font-bold text-ocean-300 text-[11px] sm:text-xs truncate block">
              {formatMoney(avgPrice)}
            </span>
          </div>
        </div>
      </div>

      {/* Anomaly Notification Banner if any */}
      {lastAnomaly && (
        <div className="p-2.5 sm:p-3 rounded-xl bg-amber-950/40 border border-amber-500/30 text-amber-300 text-xs flex items-center justify-between animate-fadeIn relative z-10">
          <div className="flex items-center gap-2">
            <span className="text-sm">⚠️</span>
            <span className="text-[11px]">
              <strong>Anomalie :</strong> {lastAnomaly.explanation} (Z={lastAnomaly.zScore.toFixed(1)})
            </span>
          </div>
          <button
            onClick={() => setLastAnomaly(null)}
            className="text-[10px] underline hover:text-white cursor-pointer ml-2"
          >
            ✕
          </button>
        </div>
      )}

      {/* Responsive Recharts Canvas */}
      <div className="w-full min-w-0 h-56 sm:h-72 md:h-80 relative z-10 overflow-hidden">
        <ResponsiveContainer width="100%" height="100%">
          <AreaChart data={currentPoints} margin={{ top: 10, right: 5, left: -25, bottom: 0 }}>
            <defs>
              <linearGradient id="oceanGradient" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor="#0284c7" stopOpacity={0.45} />
                <stop offset="95%" stopColor="#0284c7" stopOpacity={0.0} />
              </linearGradient>
            </defs>

            <CartesianGrid strokeDasharray="3 3" stroke="#132347" vertical={false} />

            <XAxis
              dataKey="time"
              stroke="#64748b"
              fontSize={10}
              tickLine={false}
              axisLine={{ stroke: "#132347" }}
              interval="preserveStartEnd"
            />

            <YAxis
              stroke="#64748b"
              fontSize={10}
              domain={["auto", "auto"]}
              orientation="right"
              tickFormatter={(v) => formatMoney(v)}
              tickLine={false}
              axisLine={{ stroke: "#132347" }}
              width={55}
            />

            <Tooltip
              contentStyle={{
                backgroundColor: "#0d1933",
                borderColor: "rgba(56, 189, 248, 0.3)",
                borderRadius: "12px",
                fontSize: 11,
                boxShadow: "0 10px 25px -5px rgba(0, 0, 0, 0.5)",
              }}
              labelStyle={{ color: "#94a3b8", marginBottom: "4px" }}
              formatter={(val: any, name: string) => [
                formatMoney(Number(val) || 0),
                name === "price" ? "Cours direct" : "SMA (7)",
              ]}
            />

            {/* High Reference Line */}
            {maxPrice > 0 && (
              <ReferenceLine
                y={maxPrice}
                stroke="#10B981"
                strokeDasharray="3 3"
                strokeOpacity={0.4}
                label={{ value: "Max", fill: "#10B981", fontSize: 9, position: "insideTopLeft" }}
              />
            )}

            {/* Low Reference Line */}
            {minPrice > 0 && (
              <ReferenceLine
                y={minPrice}
                stroke="#F43F5E"
                strokeDasharray="3 3"
                strokeOpacity={0.4}
                label={{ value: "Min", fill: "#F43F5E", fontSize: 9, position: "insideBottomLeft" }}
              />
            )}

            {/* Main Area Chart */}
            <Area
              type="monotone"
              dataKey="price"
              stroke="#38bdf8"
              strokeWidth={2}
              fillOpacity={1}
              fill="url(#oceanGradient)"
              isAnimationActive={false}
            />

            {/* Moving Average Line (SMA) */}
            {showSMA && (
              <Line
                type="monotone"
                dataKey="sma"
                stroke="#F59E0B"
                strokeWidth={1.5}
                dot={false}
                strokeDasharray="2 2"
                isAnimationActive={false}
              />
            )}
          </AreaChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}

// Helpers
function generateInitialData(basePrice: number, count: number): TickPoint[] {
  const points: TickPoint[] = [];
  let price = basePrice;
  const now = Date.now();

  for (let i = 0; i < count; i++) {
    const time = new Date(now - (count - i) * 5000).toLocaleTimeString();
    const change = (Math.random() - 0.49) * (basePrice * 0.003);
    price = +(price + change).toFixed(4);
    points.push({ index: i, time, price });
  }

  return computeMovingAverage(points, 7);
}

function computeMovingAverage(points: TickPoint[], period: number): TickPoint[] {
  return points.map((pt, idx, arr) => {
    if (idx < period - 1) {
      return { ...pt, sma: pt.price };
    }
    const window = arr.slice(idx - period + 1, idx + 1);
    const avg = window.reduce((sum, item) => sum + item.price, 0) / period;
    return { ...pt, sma: +avg.toFixed(4) };
  });
}
