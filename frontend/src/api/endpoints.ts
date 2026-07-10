import { apiClient } from "./client";
import type {
  Account, Asset, AuthResponse, BacktestResult, LeaderboardEntry, Order, OrderEvent,
  Portfolio, Position, RiskAlert, Strategy, UserProfile,
} from "./types";

export const assetsApi = {
  list: () => apiClient.get<Asset[]>("/api/assets").then((r) => r.data),
};

export const authApi = {
  register: (email: string, password: string) =>
    apiClient.post<AuthResponse>("/api/auth/register", { email, password }).then((r) => r.data),
  login: (email: string, password: string) =>
    apiClient.post<AuthResponse>("/api/auth/login", { email, password }).then((r) => r.data),
  me: () => apiClient.get<UserProfile>("/api/users/me").then((r) => r.data),
};

export const accountsApi = {
  list: () => apiClient.get<Account[]>("/api/accounts").then((r) => r.data),
  create: (type: string, displayName: string) =>
    apiClient.post<Account>("/api/accounts", { type, displayName }).then((r) => r.data),
};

export const portfoliosApi = {
  list: () => apiClient.get<Portfolio[]>("/api/portfolios").then((r) => r.data),
  get: (id: string) => apiClient.get<Portfolio>(`/api/portfolios/${id}`).then((r) => r.data),
  create: (accountId: string, name: string, mode: string, initialCashBalance: string) =>
    apiClient.post<Portfolio>("/api/portfolios", { accountId, name, mode, initialCashBalance }).then((r) => r.data),
  positions: (id: string) => apiClient.get<Position[]>(`/api/portfolios/${id}/positions`).then((r) => r.data),
};

export const ordersApi = {
  list: (portfolioId: string) =>
    apiClient.get<Order[]>("/api/orders", { params: { portfolioId } }).then((r) => r.data),
  get: (id: string) => apiClient.get<Order>(`/api/orders/${id}`).then((r) => r.data),
  events: (id: string) => apiClient.get<OrderEvent[]>(`/api/orders/${id}/events`).then((r) => r.data),
  create: (payload: {
    portfolioId: string; assetId: string; orderType: string; side: string;
    quantity: string; limitPrice?: string;
  }) => apiClient.post<Order>("/api/orders", payload).then((r) => r.data),
  cancel: (id: string, reason?: string) => apiClient.post(`/api/orders/${id}/cancel`, { reason }),
  simulateExecution: (id: string, payload: {
    quantity: string; executionPrice: string; fees: string; externalReference: string;
  }) => apiClient.post(`/api/orders/${id}/executions`, payload),
};

export const strategiesApi = {
  list: () => apiClient.get<Strategy[]>("/api/strategies").then((r) => r.data),
  get: (id: string) => apiClient.get<Strategy>(`/api/strategies/${id}`).then((r) => r.data),
  create: (payload: {
    portfolioId: string; name: string; description?: string; mode: string;
    riskLevel?: string; maximumCapital?: string; maximumLoss?: string; ruleExpression: string;
  }) => apiClient.post<Strategy>("/api/strategies", payload).then((r) => r.data),
  activate: (id: string) => apiClient.post(`/api/strategies/${id}/activate`),
  suspend: (id: string) => apiClient.post(`/api/strategies/${id}/suspend`),
  deactivate: (id: string) => apiClient.post(`/api/strategies/${id}/deactivate`),
  leaderboard: () => apiClient.get<LeaderboardEntry[]>("/api/strategies/leaderboard").then((r) => r.data),
};

export const backtestApi = {
  run: (strategyId: string, payload: {
    assetId: string; initialCapital: string; periodStart: string; periodEnd: string;
    feeRate?: string; slippageRate?: string; seed?: number;
  }) => apiClient.post<BacktestResult>(`/api/strategies/${strategyId}/backtest`, payload).then((r) => r.data),
};

export const riskApi = {
  alerts: (portfolioId: string) =>
    apiClient.get<RiskAlert[]>("/api/risk/alerts", { params: { portfolioId } }).then((r) => r.data),
};
