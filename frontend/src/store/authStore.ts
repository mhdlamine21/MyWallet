import { create } from "zustand";
import type { UserProfile } from "../api/types";

/**
 * KNOWN LIMITATION (flagged during Phase 9, not swept under the rug):
 *
 * The backend's ADR called for the refresh token to live in an httpOnly cookie and the
 * access token to live only in memory. What actually got implemented in AuthController
 * (Phase 3) returns *both* tokens in the JSON response body - no Set-Cookie header exists
 * server-side. This store therefore keeps both tokens in memory, which:
 *   - avoids the worst outcome (persisting the refresh token in localStorage, where any
 *     XSS payload could read it and mint sessions indefinitely), but
 *   - means a full page reload loses the session - the person has to log back in.
 *
 * Closing this gap needs a backend change (Set-Cookie on the refresh token, CORS
 * `credentials: true`, and a CSRF-safe design since cookies are auto-sent) - flagged as a
 * `/security-codeguard-agent` follow-up rather than patched ad hoc here.
 */
interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  user: UserProfile | null;
  setTokens: (accessToken: string, refreshToken: string) => void;
  setUser: (user: UserProfile) => void;
  clear: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  accessToken: null,
  refreshToken: null,
  user: null,
  setTokens: (accessToken, refreshToken) => set({ accessToken, refreshToken }),
  setUser: (user) => set({ user }),
  clear: () => set({ accessToken: null, refreshToken: null, user: null }),
}));
