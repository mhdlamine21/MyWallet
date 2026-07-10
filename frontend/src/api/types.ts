export interface Asset {
  id: string;
  symbol: string;
  assetClass: string;
  currency: string;
  displayName: string;
  initialPrice: string;
}

export interface AuthResponse {
  accessToken: string;
  accessTokenExpiresInSeconds: number;
  refreshToken: string;
}

export interface UserProfile {
  id: string;
  email: string;
  roles: string[];
}

export interface Account {
  id: string;
  accountType: string;
  displayName: string;
}

export interface Portfolio {
  id: string;
  accountId: string;
  name: string;
  mode: string;
  cashBalance: string;
  createdAt: string;
  updatedAt: string;
}

export interface Position {
  id: string;
  assetId: string;
  quantity: string;
  averageAcquisitionPrice: string;
}

export interface Order {
  id: string;
  portfolioId: string;
  assetId: string;
  orderType: string;
  side: "BUY" | "SELL";
  quantity: string;
  limitPrice: string | null;
  filledQuantity: string;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface OrderEvent {
  eventId: string;
  eventType: string;
  schemaVersion: number;
  occurredAt: string;
  correlationId: string;
  causationId: string | null;
  actorId: string | null;
  payload: unknown;
}

export interface Strategy {
  id: string;
  name: string;
  description: string | null;
  portfolioId: string;
  status: "DRAFT" | "ACTIVE" | "SUSPENDED" | "DEACTIVATED";
  mode: string;
  riskLevel: string;
  maximumCapital: string | null;
  maximumLoss: string | null;
  ruleExpression: string;
  createdAt: string;
  updatedAt: string;
}

export interface BacktestResult {
  id: string;
  backtestId: string;
  finalCapital: string;
  totalReturn: string;
  annualizedReturn: string;
  numberOfTrades: number;
  winRate: string;
  averageGain: string;
  averageLoss: string;
  maxDrawdown: string;
  sharpeRatio: string;
  sortinoRatio: string;
  equityCurve: number[];
  buyAndHoldEquityCurve: number[];
  monteCarloP5: number[];
  monteCarloP50: number[];
  monteCarloP95: number[];
  trades: unknown[];
}

export interface LeaderboardEntry {
  strategyId: string;
  strategyName: string;
  currentValue: string;
  baselineValue: string;
  returnFraction: string;
}

export interface RiskAlert {
  id: string;
  portfolioId: string;
  limitType: string;
  level: "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
  riskScore: number;
  explanation: string;
  raisedAt: string;
}

export interface ApiError {
  timestamp: string;
  code: string;
  message: string;
  correlationId: string;
}
