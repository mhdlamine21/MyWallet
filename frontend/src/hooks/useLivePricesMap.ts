import { useEffect, useState } from "react";
import { useStompContext } from "./StompProvider";

interface PriceTick {
  symbol: string;
  price: number;
}

/**
 * Tracks the latest live price for a dynamic set of symbols at once (e.g. every asset
 * currently held in a portfolio). Deliberately does not call {@link useStompTopic} in a
 * loop - the number of symbols can change between renders, which would violate the rules
 * of hooks; instead this calls the shared connection's plain `subscribe` function
 * (not itself a hook) inside a single `useEffect`.
 */
export function useLivePricesMap(symbols: string[]): Record<string, number> {
  const { subscribe } = useStompContext();
  const [prices, setPrices] = useState<Record<string, number>>({});
  const symbolsKey = symbols.join(",");

  useEffect(() => {
    if (!symbolsKey) return;
    const unsubscribes = symbolsKey.split(",").map((symbol) =>
      subscribe<PriceTick>(`/topic/prices/${symbol}`, (tick) => {
        setPrices((prev) => ({ ...prev, [symbol]: tick.price }));
      })
    );
    return () => unsubscribes.forEach((unsub) => unsub());
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [symbolsKey, subscribe]);

  return prices;
}
