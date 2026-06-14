# ADR 0002 - RabbitMQ over Kafka for the Event Bus

**Status:** Accepted
**Date:** 2026-09-02

## Context

The project brief allowed either RabbitMQ or Kafka for asynchronous domain events
(position updates, risk re-evaluation, notifications, audit). MyWallet is a simulator
with realistic but bounded event volume (single-digit thousands of events/day even under
a busy public demo), operated solo, and deployed partly on a free-tier cloud host with
tight RAM constraints.

## Decision

Use **RabbitMQ** (topic exchange, one queue per consumer group: positions, risk,
notifications, audit).

## Consequences

- Simpler operational model: no Zookeeper/KRaft, no partition/offset management.
- Lower memory footprint - fits comfortably in a free-tier cloud instance alongside
  Postgres and Redis (validated by the Phase 0/10 spike).
- Pub/sub semantics via exchanges map directly onto "one event, many independent
  consumers" without extra plumbing.
- Trade-off accepted: RabbitMQ does not give the same replay-from-offset guarantees as
  Kafka. This is fine here because **the event store of record is the `domain_events`
  Postgres table**, not the queue - RabbitMQ is used purely for asynchronous fan-out of
  already-persisted events, never as the durable log itself.
- If a future need arises to demonstrate Kafka specifically (e.g. for a job application
  emphasizing streaming systems), this ADR should be revisited rather than silently
  reversed.
