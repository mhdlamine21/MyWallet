import { createContext, useCallback, useContext, useEffect, useRef, useState, type ReactNode } from "react";
import { Client, type IMessage, type StompSubscription } from "@stomp/stompjs";
import { useAuthStore } from "../store/authStore";

const wsBaseUrl = (import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080").replace(/^http/, "ws");

interface SubscriptionEntry {
  stompSubscription: StompSubscription | null;
  callbacks: Set<(payload: unknown) => void>;
}

interface StompContextValue {
  connected: boolean;
  subscribe: <T>(destination: string, onMessage: (payload: T) => void) => () => void;
}

const StompContext = createContext<StompContextValue | null>(null);

/**
 * Exactly one WebSocket/STOMP connection for the whole app, however many components
 * subscribe to however many topics. Replaces the earlier one-connection-per-hook-call
 * design (fine for a single subscription per page, wasteful the moment a page needs
 * several live topics at once - e.g. a portfolio's risk-alerts, orders, and a live price
 * per held asset, all at the same time).
 *
 * On reconnect (network blip, backend restart), every currently-registered destination is
 * automatically re-subscribed - a component using {@link useStompTopic} doesn't need to
 * know or care that a reconnect happened.
 */
export function StompProvider({ children }: { children: ReactNode }) {
  const accessToken = useAuthStore((s) => s.accessToken);
  const [connected, setConnected] = useState(false);
  const clientRef = useRef<Client | null>(null);
  const subscriptionsRef = useRef<Map<string, SubscriptionEntry>>(new Map());

  const resubscribeAll = useCallback((client: Client) => {
    subscriptionsRef.current.forEach((entry, destination) => {
      entry.stompSubscription = client.subscribe(destination, (message: IMessage) => {
        let payload: unknown;
        try {
          payload = JSON.parse(message.body);
        } catch {
          return; // malformed payload - ignore rather than crash every subscriber
        }
        entry.callbacks.forEach((cb) => cb(payload));
      });
    });
  }, []);

  useEffect(() => {
    if (!accessToken) {
      clientRef.current?.deactivate();
      clientRef.current = null;
      setConnected(false);
      return;
    }

    const client = new Client({
      brokerURL: `${wsBaseUrl}/ws?token=${encodeURIComponent(accessToken)}`,
      reconnectDelay: 3000,
      onConnect: () => {
        setConnected(true);
        resubscribeAll(client);
      },
      onDisconnect: () => setConnected(false),
      onStompError: () => setConnected(false),
    });

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
      clientRef.current = null;
    };
  }, [accessToken, resubscribeAll]);

  const subscribe = useCallback(<T,>(destination: string, onMessage: (payload: T) => void) => {
    let entry = subscriptionsRef.current.get(destination);
    if (!entry) {
      entry = { stompSubscription: null, callbacks: new Set() };
      subscriptionsRef.current.set(destination, entry);
      if (clientRef.current?.connected) {
        entry.stompSubscription = clientRef.current.subscribe(destination, (message: IMessage) => {
          let payload: unknown;
          try {
            payload = JSON.parse(message.body);
          } catch {
            return;
          }
          entry!.callbacks.forEach((cb) => cb(payload));
        });
      }
    }

    const callback = onMessage as (payload: unknown) => void;
    entry.callbacks.add(callback);

    return () => {
      entry!.callbacks.delete(callback);
      if (entry!.callbacks.size === 0) {
        entry!.stompSubscription?.unsubscribe();
        subscriptionsRef.current.delete(destination);
      }
    };
  }, []);

  return <StompContext.Provider value={{ connected, subscribe }}>{children}</StompContext.Provider>;
}

export function useStompContext(): StompContextValue {
  const ctx = useContext(StompContext);
  if (!ctx) {
    throw new Error("useStompContext must be used within a <StompProvider>");
  }
  return ctx;
}
