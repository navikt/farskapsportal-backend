-- Table: signeringsinformasjon

/*
ALTER TABLE public.signeringsinformasjon
    DROP COLUMN sendt_til_signering;
 */

ALTER TABLE public.signeringsinformasjon
    ADD COLUMN sendt_til_signering timestamp without time zone;
