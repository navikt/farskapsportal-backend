-- Table: dokumentinnhold

/*** revert **

CREATE TABLE dokumentinnhold
(
    id integer NOT NULL GENERATED BY DEFAULT AS IDENTITY ( INCREMENT 1 START 1 MINVALUE 1 MAXVALUE 2147483647 CACHE 1 ),
    innhold bytea,
    CONSTRAINT dokumentinnhold_pkey PRIMARY KEY (id)
)

    TABLESPACE pg_default;

 DELETE FROM flyway_schema_history WHERE version = '16.0.2';
 */

DROP TABLE public.dokumentinnhold;