-- Table: public.status_kontrollere_far

-- DROP TABLE public.status_kontrollere_far;

CREATE TABLE public.status_kontrollere_far
(
    id integer NOT NULL,
    fnr_mor integer NOT NULL,
    antall_feilede_forsoek integer NOT NULL default 0,
    tidspunkt_siste_feilede_forsoek timestamp without time zone,
    CONSTRAINT status_kontrollere_far_pkey PRIMARY KEY (id)
)

TABLESPACE pg_default;

GRANT ALL ON TABLE public.status_kontrollere_far TO cloudsqliamuser;