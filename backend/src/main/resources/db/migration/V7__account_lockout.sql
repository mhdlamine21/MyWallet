-- V7__account_lockout.sql
-- Added during the /security-codeguard-agent pass: no account-level brute-force
-- protection existed before this (only the IP-based rate limiter, a separate defense).

ALTER TABLE users
    ADD COLUMN failed_login_attempts INT NOT NULL DEFAULT 0,
    ADD COLUMN locked_until TIMESTAMPTZ;
