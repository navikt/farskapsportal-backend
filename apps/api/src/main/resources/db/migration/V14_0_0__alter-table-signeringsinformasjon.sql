-- Table: signeringsinformasjon

/*** revert **

 ALTER TABLE public.signeringsinformasjon ADD COLUMN xades_url varchar(255);
 DELETE FROM flyway_schema_history WHERE version = '14.0.0';
 */

ALTER TABLE public.signeringsinformasjon DROP COLUMN xades_url;