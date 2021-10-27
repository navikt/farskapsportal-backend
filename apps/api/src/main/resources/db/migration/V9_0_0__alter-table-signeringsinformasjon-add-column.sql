-- Table: signeringsinformasjon

/*
    ALTER TABLE public.signeringsinformasjon
         DROP COLUMN status_signering;
 */

ALTER TABLE public.signeringsinformasjon
    ADD COLUMN status_signering varchar(20);