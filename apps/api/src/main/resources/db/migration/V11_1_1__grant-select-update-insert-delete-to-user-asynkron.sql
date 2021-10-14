-- Lese og skrivetilgang til alle tabeller i public-skjema for ${user.asynkron}

/*
REVOKE SELECT, INSERT, UPDATE, DELETE
    ON ALL TABLES IN SCHEMA public
    FROM ${user.asynkron};
 */

GRANT SELECT, INSERT, UPDATE, DELETE
    ON ALL TABLES IN SCHEMA public
    TO ${user.asynkron};
