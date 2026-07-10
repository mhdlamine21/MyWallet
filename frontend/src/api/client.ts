import axios, { type AxiosError, type InternalAxiosRequestConfig } from "axios";
import { useAuthStore } from "../store/authStore";
import type { AuthResponse } from "./types";

const baseURL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

export const apiClient = axios.create({ baseURL });

apiClient.interceptors.request.use((config) => {
  const { accessToken } = useAuthStore.getState();
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }
  return config;
});

let refreshInFlight: Promise<string> | null = null;

async function refreshAccessToken(): Promise<string> {
  const { refreshToken, setTokens, clear } = useAuthStore.getState();
  if (!refreshToken) {
    clear();
    throw new Error("No refresh token available");
  }

  const response = await axios.post<AuthResponse>(`${baseURL}/api/auth/refresh`, { refreshToken });
  setTokens(response.data.accessToken, response.data.refreshToken);
  return response.data.accessToken;
}

// Retries a request exactly once after a successful silent refresh - if the refresh
// itself fails (expired/revoked/reused token), the session is cleared and the person is
// sent back to the login screen rather than looping.
apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as (InternalAxiosRequestConfig & { _retried?: boolean }) | undefined;

    if (error.response?.status === 401 && originalRequest && !originalRequest._retried) {
      originalRequest._retried = true;
      try {
        refreshInFlight ??= refreshAccessToken().finally(() => {
          refreshInFlight = null;
        });
        const newAccessToken = await refreshInFlight;
        originalRequest.headers = originalRequest.headers ?? {};
        originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
        return apiClient.request(originalRequest);
      } catch {
        useAuthStore.getState().clear();
        window.location.assign("/login");
      }
    }
    return Promise.reject(error);
  }
);
