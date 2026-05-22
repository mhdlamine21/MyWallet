# ADR 0004 - EventMetadata as a Composed Value Object

**Status:** Accepted
**Date:** 2026-09-03
**Raised during:** `/improve-architecture` SCAN review, before Phase 2

## Context

The project brief specifies ~20 domain events across order, portfolio, strategy, and risk
modules. Each event needs the same 5 metadata fields (`eventId`, `occurredAt`,
`correlationId`, `causationId`, `actorId`) in addition to its own payload fields. Declaring
these 5 fields on every event record, as the first `OrderCreated` implementation did,
would duplicate the same boilerplate ~20 times and risk field-ordering or omission
mistakes creeping in as more events are added.

## Decision

Extract `EventMetadata` as its own record (`eventId, occurredAt, correlationId,
causationId, actorId`), composed as the first field of every event record. `DomainEvent`
exposes `metadata()` plus default methods that delegate to it, so callers (event store,
audit log, correlation-id logging) keep calling `event.eventId()`, `event.correlationId()`
etc. exactly as before - this refactor is invisible to every caller outside the event
records themselves.

## Consequences

- Every future event (`PortfolioCreated`, `RiskLimitBreached`, `StrategyActivated`, ...)
  composes `EventMetadata` instead of repeating 5 fields - one declaration, ~20 use sites.
- `EventMetadata.create(correlationId, causationId, actorId)` is the single place that
  decides how `eventId` and `occurredAt` are generated, making it trivial to, for example,
  inject a fixed clock in tests later if needed.
- Slight indirection cost: reading an event's `eventId()` now goes through one default
  method hop instead of being a direct record component - negligible at this scale.
- `Order.reconstruct()` was changed to accept `List<DomainEvent>` instead of
  `Iterable<DomainEvent>`, removing an unchecked cast that was flagged in the same review.
