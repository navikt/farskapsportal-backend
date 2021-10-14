-- Lese og skrivetilgang til alle tabeller i public-skjema for user

/*
REVOKE SELECT, INSERT, UPDATE, DELETE
    ON ALL TABLES IN SCHEMA public
    FROM ${user};
 */

GRANT SELECT, INSERT, UPDATE, DELETE
    ON ALL TABLES IN SCHEMA public
    TO ${FLYWAY_PLACEHOLDERS_USER};
