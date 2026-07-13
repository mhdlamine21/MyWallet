import { useState } from "react";
import { useStompTopic } from "../hooks/useStompTopic";
import { useCurrencyStore } from "../store/useCurrencyStore";

interface TickerProps {
  symbol: string;
  name: string;
  initialPrice: number;
  icon: string;
}

const DEFAULT_TICKERS: TickerProps[] = [
  { symbol: "BTCUSDT", name: "Bitcoin", initialPrice: 60000, icon: "₿" },
  { symbol: "ETHUSDT", name: "Ethereum", initialPrice: 3000, icon: "Ξ" },
  { symbol: "AAPL", name: "Apple Inc.", initialPrice: 190, icon: "🍎" },
  { symbol: "EURUSD", name: "Euro / Dollar", initialPrice: 1.08, icon: "💶" },
];

export default function LiveMarketTickerTape() {
  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3 md:gap-4">
      {DEFAULT_TICKERS.map((t) => (
        <TickerCard key={t.symbol} {...t} />
      ))}
    </div>
  );
}

function TickerCard({ symbol, name, initialPrice, icon }: TickerProps) {
  const [price, setPrice] = useState<number>(initialPrice);
  const [prevPrice, setPrevPrice] = useState<number>(initialPrice);
  const [sparkline, setSparkline] = useState<number[]>([
    initialPrice * 0.99,
    initialPrice * 0.995,
    initialPrice,
    initialPrice * 1.002,
    initialPrice * 1.001,
    initialPrice,
  ]);
  const { formatMoney } = useCurrencyStore();

  useStompTopic<{ symbol: string; price: number }>(`/topic/prices/${symbol}`, (tick) => {
    setPrevPrice(price);
    setPrice(tick.price);
    setSparkline((prev) => [...prev.slice(-10), tick.price]);
  });

  const diff = price - initialPrice;
  const pctChange = ((diff / initialPrice) * 100).toFixed(2);
  const isUp = diff >= 0;
  const isTickUp = price >= prevPrice;

  return (
    <div className="p-4 rounded-2xl bg-[#0d1933] border border-ocean-500/20 hover:border-ocean-500/40 transition-all shadow-md group relative overflow-hidden">
      {/* Background glow */}
      <div
        className={`absolute -right-10 -bottom-10 w-24 h-24 rounded-full blur-2xl pointer-events-none transition-colors ${
          isUp ? "bg-emerald-500/10" : "bg-rose-500/10"
        }`}
      />

      <div className="flex items-center justify-between mb-2">
        <div className="flex items-center gap-2">
          <span className="w-7 h-7 rounded-lg bg-navy-850 border border-ocean-500/20 flex items-center justify-center text-sm shadow-sm">
            {icon}
          </span>
          <div>
            <span className="text-xs font-bold text-white block leading-tight">{symbol}</span>
            <span className="text-[10px] text-slate-400 block leading-tight truncate max-w-[90px]">{name}</span>
          </div>
        </div>

        {/* Live indicator badge */}
        <span
          className={`text-[10px] font-mono font-bold px-2 py-0.5 rounded-full flex items-center gap-1 border ${
            isUp
              ? "bg-emerald-950/60 text-emerald-400 border-emerald-500/30"
              : "bg-rose-950/60 text-rose-400 border-rose-500/30"
          }`}
        >
          <span>{isUp ? "▲" : "▼"}</span>
          <span>{isUp ? `+${pctChange}%` : `${pctChange}%`}</span>
        </span>
      </div>

      {/* Price display with tick animation */}
      <div className="flex items-baseline justify-between mt-3">
        <span
          className={`text-base md:text-lg font-bold font-mono transition-colors duration-300 ${
            isTickUp ? "text-emerald-300" : "text-rose-300"
          }`}
        >
          {formatMoney(price)}
        </span>

        {/* Mini SVG Sparkline */}
        <div className="w-16 h-7">
          <svg className="w-full h-full overflow-visible" viewBox="0 0 60 25">
            {renderSparkline(sparkline, isUp)}
          </svg>
        </div>
      </div>
    </div>
  );
}

function renderSparkline(data: number[], isUp: boolean) {
  if (data.length < 2) return null;
  const min = Math.min(...data);
  const max = Math.max(...data);
  const range = max - min || 1;

  const points = data
    .map((val, idx) => {
      const x = (idx / (data.length - 1)) * 60;
      const y = 23 - ((val - min) / range) * 20;
      return `${x},${y}`;
    })
    .join(" ");

  const color = isUp ? "#10B981" : "#F43F5E";

  return (
    <>
      <polyline fill="none" stroke={color} strokeWidth="1.75" points={points} strokeLinecap="round" strokeLinejoin="round" />
      <circle
        cx={(data.length - 1) * (60 / (data.length - 1))}
        cy={23 - ((data[data.length - 1] - min) / range) * 20}
        r="2"
        fill={color}
      />
    </>
  );
}
