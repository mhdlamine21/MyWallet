# ADR 0003 - Event Store as a Postgres Table, Not a Dedicated Event Store

**Status:** Accepted
**Date:** 2026-09-02

## Context

Event sourcing for `Order` and `Portfolio` needs a durable, append-only, ordered log of
`DomainEvent`s that supports replay by `aggregateId` and point-in-time queries. Options
considered: a dedicated event store (EventStoreDB, Axon Server) vs. a plain
`domain_event` table in the existing PostgreSQL instance.

## Decision

Use a single `domain_event` table in PostgreSQL, with a unique composite index on
`(aggregate_id, event_version)` and a secondary index on `occurred_at`.

## Consequences

- One fewer infrastructure component to run, configure, and pay for - meaningful for a
  solo project targeting a free-tier cloud demo.
- ACID guarantees for free: an aggregate's event append and any same-transaction
  projection update can share a transaction boundary when needed.
- Optimistic concurrency is implemented directly via the unique index rejecting a
  duplicate `(aggregate_id, event_version)` insert - no separate concurrency mechanism
  needed.
- Trade-off accepted: no built-in event-store features like subscriptions-by-category or
  built-in projections - these are implemented explicitly in the application layer
  (`PositionProjector`, etc.), which is more code but keeps the domain framework-free per
  the hexagonal architecture rule.
- If event volume ever became large enough to need horizontal scaling of the log itself,
  this decision would need revisiting - explicitly out of scope for a demo-scale project.
