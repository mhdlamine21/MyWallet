import { useEffect, useRef } from "react";
import { useStompContext } from "./StompProvider";

/**
 * Subscribes to one STOMP destination over the app-wide shared connection (see
 * {@code StompProvider}). Safe to call many times across many components - each call adds
 * one more listener to the shared subscription for that destination rather than opening a
 * new connection.
 */
export function useStompTopic<T>(destination: string | null, onMessage: (payload: T) => void) {
  const { connected, subscribe } = useStompContext();
  const onMessageRef = useRef(onMessage);
  onMessageRef.current = onMessage;

  useEffect(() => {
    if (!destination) return;
    return subscribe<T>(destination, (payload) => onMessageRef.current(payload));
  }, [destination, subscribe]);

  return { connected };
}
