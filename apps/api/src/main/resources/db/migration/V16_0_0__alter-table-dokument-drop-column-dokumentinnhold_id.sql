-- Table: dokument

/*** revert **

 ALTER TABLE public.dokument
    ADD COLUMN dokumentinnhold_id integer,
    ADD CONSTRAINT fk_dokumentinnhold FOREIGN KEY (dokumentinnhold_id)
        REFERENCES dokumentinnhold (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE CASCADE;

 DELETE FROM flyway_schema_history WHERE version = '16.0.0';
 */

ALTER TABLE public.dokument
    DROP COLUMN dokumentinnhold_id;