-- Table: farskapserklaering

/*
ALTER TABLE public.farskapserklaering
    DROP COLUMN oppgave_sendt,
    ADD COLUMN joark_journalpost_id varchar(50),
    ADD COLUMN sendt_til_joark timestamp without time zone;

delete from flyway_schema_history where version = '12.0.0';
 */

ALTER TABLE public.farskapserklaering
    DROP COLUMN joark_journalpost_id,
    DROP COLUMN sendt_til_joark,
    ADD COLUMN oppgave_sendt timestamp without time zone;
