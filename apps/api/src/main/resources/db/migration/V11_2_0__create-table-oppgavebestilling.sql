-- Table: oppgavebestilling

-- DROP TABLE oppgavebestilling;

CREATE TABLE oppgavebestilling
(
    id integer NOT NULL GENERATED BY DEFAULT AS IDENTITY ( INCREMENT 1 START 1 MINVALUE 1 MAXVALUE 2147483647 CACHE 1 ),
    farskapserklaering_id integer,
    forelder_id integer,
    event_id varchar(255),
    opprettet timestamp without time zone,
    ferdigstilt timestamp without time zone,
    CONSTRAINT oppgavebestilling_pkey PRIMARY KEY (id),
    CONSTRAINT uk_oppgavebestilling_event_id UNIQUE (event_id)
)

TABLESPACE pg_default;