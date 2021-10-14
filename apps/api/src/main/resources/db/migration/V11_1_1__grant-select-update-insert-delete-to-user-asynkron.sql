-- Lese og skrivetilgang til alle tabeller i public-skjema for user_asynkron

/*
REVOKE SELECT, INSERT, UPDATE, DELETE
    ON ALL TABLES IN SCHEMA public
    FROM ${user_asynkron};
 */

GRANT SELECT, INSERT, UPDATE, DELETE
    ON ALL TABLES IN SCHEMA public
    TO ${user_asynkron};
