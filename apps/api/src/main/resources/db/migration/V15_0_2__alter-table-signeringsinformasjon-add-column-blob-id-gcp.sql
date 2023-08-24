-- Table: dokument

/*** revert **

 ALTER TABLE public.signeringsinformasjon DROP COLUMN blob_id_xades;
 DELETE FROM flyway_schema_history WHERE version = '15.0.2';
 */

ALTER TABLE public.signeringsinformasjon
    ADD COLUMN blob_id_xades integer,
    ADD CONSTRAINT fk_blob_id_gcp_id FOREIGN KEY (blob_id_xades)
        REFERENCES blob_id_gcp (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION;