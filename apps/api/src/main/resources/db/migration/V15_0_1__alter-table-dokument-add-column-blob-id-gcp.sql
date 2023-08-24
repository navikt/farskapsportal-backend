-- Table: dokument

/*** revert **

 ALTER TABLE public.dokument DROP COLUMN blob_id_pades;
 DELETE FROM flyway_schema_history WHERE version = '15.0.1';
 */

ALTER TABLE public.dokument
    ADD COLUMN blob_id_pades integer,
    ADD CONSTRAINT fk_blob_id_gcp_id FOREIGN KEY (blob_id_pades)
        REFERENCES blob_id_gcp (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION;