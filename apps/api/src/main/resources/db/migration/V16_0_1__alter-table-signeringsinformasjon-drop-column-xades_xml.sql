-- Table: signeringsinformasjon

/*** revert **

ALTER TABLE public.signeringsinformasjon
    ADD COLUMN xades_xml bytea;

 DELETE FROM flyway_schema_history WHERE version = '16.0.1';
 */

ALTER TABLE public.signeringsinformasjon
    DROP COLUMN xades_xml;