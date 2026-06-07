-- Two isolated databases, one per service — ADR-0003.
-- No cross-database foreign keys or queries: app.users references the auth user
-- id by value only. Each service connects with its own credentials.
-- Runs once, on first container init (empty data dir).

CREATE USER auth_user WITH PASSWORD 'auth';
CREATE USER app_user  WITH PASSWORD 'app';

CREATE DATABASE auth OWNER auth_user;
CREATE DATABASE app  OWNER app_user;
