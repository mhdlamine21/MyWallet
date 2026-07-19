# Sequence - Event Replay & Point-in-Time Reconstruction

Two use cases share the same mechanism: reconstructing an aggregate's *current* state
(used on every command), and reconstructing a Portfolio's state *as of a past date*
(used by `GET /api/portfolios/{id}/performance?at=...`).

```mermaid
sequenceDiagram
    actor Client
    participant API as PortfolioController
    participant App as ReconstructPortfolioService
    participant Store as EventStoreRepository
    participant Portfolio as Portfolio (aggregate)

    Client->>API: GET /api/portfolios/{id}?at=2026-06-01T00:00:00Z
    API->>App: ReconstructAtQuery(portfolioId, at)
    App->>Store: loadHistory(portfolioId, until = at)
    Note over Store: SELECT * FROM domain_event<br/>WHERE aggregate_id = :id AND occurred_at <= :at<br/>ORDER BY event_version ASC
    Store-->>App: List~DomainEvent~ (ordered, gapless)
    App->>Portfolio: Portfolio.reconstruct(id, history)
    loop for each event in history
        Portfolio->>Portfolio: apply(event)
        Note over Portfolio: Pure in-memory mutation.<br/>No DB write happens here.
    end
    Portfolio-->>App: state as of `at`
    App-->>API: PortfolioSnapshot DTO
    API-->>Client: 200 OK
```

## Notes

- Replaying from event #1 every time does not scale indefinitely; if a spike test
  (see `plan-project` -> Technical Spikes) shows replay latency becoming a problem, the
  mitigation is **snapshotting**: periodically persist `(aggregateId, version, stateBlob)`
  and replay only the events *after* the last snapshot.
- The same mechanism powers the **Mode Replay** UI feature: instead of jumping straight to
  the final state, the frontend requests events in small time-windowed batches and applies
  them incrementally over WebSocket, at a configurable playback speed, so the user watches
  the reconstruction happen rather than seeing only the end result.
