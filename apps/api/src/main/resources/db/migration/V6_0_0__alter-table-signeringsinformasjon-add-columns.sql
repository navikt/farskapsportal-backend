-- Table: signeringsinformasjon

/*
    ALTER TABLE public.signeringsinformasjon
         DROP COLUMN xades_url,
         DROP COLUMN xades_xml;
 */

ALTER TABLE public.signeringsinformasjon
    ADD COLUMN xades_url varchar(255),
    ADD COLUMN xades_xml bytea;
