-- Table: gcp_blob_id

-- DROP TABLE blob_id_gcp;
-- DELETE FROM flyway_schema_history WHERE version = '15.0.0';

CREATE TABLE blob_id_gcp
(
    id integer NOT NULL GENERATED BY DEFAULT AS IDENTITY ( INCREMENT 1 START 1 MINVALUE 1 MAXVALUE 2147483647 CACHE 1 ),
    bucket varchar(255),
    encryption_key_version integer,
    generation bigint,
    name varchar(255),
    CONSTRAINT blob_id_gcp_pkey PRIMARY KEY (id)
)

TABLESPACE pg_default;