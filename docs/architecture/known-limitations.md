# MyWallet - Known Limitations

Consolidated in one place rather than scattered across code comments (though the comments
still exist at each specific decision point, cross-referenced below). This is an honest
account, written as the project was built - nothing here was discovered by a reviewer
after the fact.

## Intelligent Detection (Phase 14)

- **Price anomaly detection uses a fixed z-score threshold (4.0 by default)**, not an
  adaptive one - a genuinely more volatile asset (higher configured GBM volatility) will
  trip it more often than a calmer one, by construction. A more sophisticated version
  would normalize the threshold per asset's own configured volatility rather than using
  one global constant.
- **Behavioral anomaly detection needs a minimum population of 3 portfolios** before it
  runs at all - comparing one portfolio's behavior to a population of one or two isn't a
  meaningful signal. On a fresh/small demo deployment, this detector may simply never
  fire until enough portfolios exist.
- **The Isolation Forest is refit from scratch every cycle** (every 30 seconds by
  default) over whatever portfolios exist at that moment - this is deliberately
  unsupervised and adaptive (see `BehaviorAnomalyDetectionService`'s javadoc), but it also
  means "anomalous" is always relative to the *current* population, not some fixed
  historical baseline - a portfolio that looks normal today could look anomalous
  tomorrow purely because everyone else's behavior shifted, not because it changed itself.
- **Feature set for behavioral detection is intentionally small** (orders placed today,
  largest single-asset exposure fraction, total portfolio value) - richer features
  (order size distribution, time-of-day patterns, win/loss streaks) were left out to keep
  this phase's scope contained; the `IsolationForest`/`IsolationTree` implementation
  itself supports any number of features without changes, so extending this is additive.
- **30-minute alert cooldown per portfolio** prevents re-flagging the same ongoing
  anomalous state every 30-second cycle, but also means a portfolio's behavior could
  shift from anomalous to fine and back within that window without a fresh alert either way.

## Bots / Automatic Strategy Execution (Phase 12)

- **The leaderboard is intentionally public across all users, not privacy-scoped.**
  `GET /api/strategies/leaderboard` and `/topic/leaderboard` return every `ACTIVE`
  strategy's name and return-since-activation, regardless of who owns it - any
  authenticated user sees everyone's strategy performance. This is a deliberate design
  choice (a leaderboard is a shared, game-like ranking by definition - restricting it to
  "your own strategies" would defeat the point of the feature), not an oversight, but it's
  worth being explicit about: in a real product handling real trading performance, this
  would need an opt-in ("show my strategy on the public leaderboard") rather than being
  on by default. For an educational simulator with fake money, the trade-off favors the
  more interesting demo experience.
- **Crash-window risk in `StrategyExecutionScheduler.evaluateOne`**: documented in the
  method's own javadoc - a process crash in the narrow gap between a successful
  order+fill and saving the strategy's long/flat execution state could cause the next
  tick to issue a duplicate order for the same signal. Accepted for a demo project;
  closing it properly needs an outbox-pattern-style atomic unit spanning the order,
  the fill, and the state update.
- **Position sizing is all-in/all-out only** (100% of available cash on entry, 100% of
  the held position on exit) - matches `BacktestEngine`'s own model so live results stay
  comparable to a backtest of the same rule, but is not a realistic risk-managed sizing
  strategy (no partial entries, no scaling in/out, no Kelly-criterion sizing - still on
  the Phase 7b backlog).
- **Auto-generated orders instantly self-fill** via `RecordExecutionService` right after
  creation - there's no real order book or matching engine to wait on in a simulator, so
  this is presented as the honest model of what's actually happening, not dressed up as
  more realistic than it is.

## Real-time (WebSocket)

- ~~Topic subscriptions authenticated but not authorized per-portfolio~~ - **fixed** during
  the `/security-codeguard-agent` pass: `StompSubscriptionAuthorizationInterceptor` now
  rejects a `SUBSCRIBE` to `/topic/portfolios/{portfolioId}/...` unless the connected user
  owns that portfolio, mirroring the "404, not 403" pattern used everywhere else.
- The frontend's WebSocket connection is established once with the access token at connect
  time and does not reconnect with a refreshed token if the original one expires mid-session
  - acceptable today since access tokens outlive a typical demo session, but worth revisiting
  if the access-token TTL is ever shortened.
- No message replay/catch-up on reconnect: a client that briefly disconnects misses
  whatever was broadcast during the gap (the next React Query refetch on reconnect or next
  page load still shows correct data - this only affects the live-push experience, not
  correctness).

## Security

- **Process note:** `/security-codeguard-agent` was not run per-phase as originally
  intended - it was run once, retroactively, after Phase 11 (WebSocket). Going forward it
  runs at the end of every phase. This document's strikethrough entries above are what
  that first full pass found and fixed immediately; it did not attempt automated
  dependency-CVE scanning (no Maven Central access in the build sandbox - run
  `mvn dependency-check:check` or enable GitHub Dependabot before any real deployment).
- ~~No rate limiting on auth endpoints~~ - **fixed**: `AuthRateLimitFilter`, in-memory
  sliding window, 10 requests/IP/minute by default on `/api/auth/**`. Single-node only
  (see the filter's own javadoc for the trade-off if this ever runs behind a load balancer
  with more than one backend instance).
- ~~No account lockout after failed login attempts~~ - **fixed**: `UserEntity` now locks
  for 15 minutes after 5 consecutive failed attempts, with the same generic error message
  used whether the account is locked, doesn't exist, or the password was simply wrong.
- ~~JWT secret could silently default to a public placeholder value~~ - **fixed**:
  `JwtSecretSafetyCheck` refuses to start the application if the configured secret is a
  known placeholder or under 32 characters, unless running in a recognized local-dev
  profile. This was actually CRITICAL until fixed - the placeholder secrets are visible in
  this public repo, and anyone who read them could have forged a valid JWT for any user,
  including ADMIN, on any deployment that forgot to override `JWT_SECRET`.
- ~~WebSocket CORS accepted any origin~~ - **fixed**: now uses the same
  `mywallet.cors.allowed-origins` allow-list as the HTTP layer.
- **Refresh token is not in an httpOnly cookie**, despite that being the original
  architectural decision (see `docs/architecture/adr/` discussion during Phase 3). The
  actual `AuthController` returns both tokens in the JSON body. The frontend keeps both in
  memory (not `localStorage`) as a partial mitigation, but this means a full page reload
  loses the session. **Fix requires a backend change**: `Set-Cookie` on the refresh token,
  CORS `credentials: true`, and a CSRF-safe design since cookies are auto-sent. Flagged for
  `/security-codeguard-agent`.
- **Rate limiting is not implemented.** No throttling exists on `/api/auth/login`,
  `/api/auth/register`, or any other endpoint. A public demo deployment needs this before
  going live (see the project's original security requirements list).
- No account lockout after repeated failed login attempts.
- `CancelOrderService`'s ownership check duplicates logic that also exists in
  `PortfolioApplicationService` and `CreateOrderService` - three near-identical
  "load portfolio -> load account -> check owner" blocks. Not wrong, but a candidate for
  extraction into a shared `PortfolioOwnershipGuard` if `/improve-architecture` revisits it.

## Domain / Financial Modeling

- **Portfolio cash balance is not event-sourced.** `PositionUpdateListener` mutates
  `PortfolioProjectionEntity.cashBalance` directly rather than the `Portfolio` aggregate
  raising a `CashSettled`-style event. This means the cash balance's history isn't
  independently replayable from `domain_events` the way `Order` and the rest of
  `Portfolio`'s own state are - a real gap in the "everything is event-sourced" story for
  this specific field. Documented in `Portfolio.java`'s class javadoc as a deliberate
  Phase 5 scope decision, not an oversight discovered later - but still a gap.
- **Backtesting is single-asset with a binary long/flat signal.** No position sizing
  tiers, no short-selling, no multi-asset portfolios in a single backtest run. The
  original brief's "plusieurs actifs" backtest requirement is not met.
- **RiskEngine ships 5 of the 12 checks** from the original brief: available cash,
  available quantity for sale, max order value, max exposure per asset, max orders per
  day. Missing: max exposure per sector, max daily/weekly loss, max drawdown, max leverage,
  max volatility, portfolio concentration, correlation between assets. These need
  historical portfolio-value tracking infrastructure this phase doesn't build.
- **VaR (historical/parametric) and Kelly-criterion position sizing** - proposed during
  the feature brainstorm as high-value additions - are not implemented.
- **No automatic strategy-driven order generation.** `Strategy` and its rule expression
  can be created, activated, suspended, and backtested, but an `ACTIVE` strategy does not
  automatically place live orders when its rule fires. That wiring (and the leaderboard
  differentiator that was meant to sit on top of it) is deferred.
- Only SMA, EMA, and RSI are implemented in the rule engine. MACD, Bollinger Bands, ATR,
  VWAP, and OBV are parseable (clear error, not silent misbehavior) but not evaluable.
- Reconciliation with simulated external platforms is not implemented.
- Automatic strategy suspension on a risk breach is not implemented - `RiskAlert`s are
  recorded, but nothing currently suspends the strategy that (indirectly) caused one.

## Frontend

- Design is deliberately sober (per an early discovery decision: 80% effort on
  backend/API robustness, 20% on a functional-but-plain frontend) - no charting beyond
  the one equity-curve chart, no dark/light theme toggle beyond the single dark theme, no
  extensive empty/loading/error states on every single screen.
- No WebSocket integration - the frontend polls via React Query's normal refetching, not
  live price ticks or real-time order/alert push. `RabbitMqConfig`'s exchange has capacity
  for this later without a redesign, but the socket layer itself doesn't exist yet.
- No admin UI for the kill switch or user/role management - those exist as API endpoints
  only, usable via Swagger/curl, not through a dedicated screen.

## Infrastructure / Deployment

- Never actually run in a real Docker environment or against real Maven Central during
  development (the build sandbox had neither Docker nor Maven Central access) - code was
  written and reasoned about carefully and self-reviewed for the bugs that were caught,
  but `mvn clean verify` and `docker compose up` have not been executed end-to-end by
  Claude. **Run them locally before treating this as verified.**
- The frontend, by contrast, *was* verified in-sandbox: `npm install`, `npx tsc -b`, and
  `npm run build` all ran successfully, and one real compilation error (`ImportMeta.env`
  needing `vite/client` types) was caught and fixed this way.
- No horizontal scaling considered anywhere (single-node Postgres, single RabbitMQ node).
- Public cloud demo deployment (Railway/Render/Oracle Free Tier) has not actually been
  performed - `docker-compose.prod-lite.yml` and the resource limits in it are a design,
  not a validated configuration.
