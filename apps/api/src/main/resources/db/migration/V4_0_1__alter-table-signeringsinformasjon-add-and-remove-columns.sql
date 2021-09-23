-- Table: signeringsinformasjon

/*
    ALTER TABLE public.signeringsinformasjon
         ADD COLUMN status_url varchar(255);
 */

ALTER TABLE public.signeringsinformasjon
    DROP COLUMN status_url;
