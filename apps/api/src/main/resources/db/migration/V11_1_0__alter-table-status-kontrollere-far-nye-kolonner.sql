-- Table: status_kontrollere_far

/*
ALTER TABLE public.status_kontrollere_far
    DROP COLUMN registrert_navn_far,
    DROP COLUMN oppgitt_navn_far;
 */

ALTER TABLE public.status_kontrollere_far
    ADD COLUMN registrert_navn_far varchar(255),
    ADD COLUMN oppgitt_navn_far varchar(255);
