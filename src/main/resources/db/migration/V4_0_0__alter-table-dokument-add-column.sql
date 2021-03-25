-- Table: dokument

/*
    ALTER TABLE public.dokument
         DROP COLUMN bekreftelses_url;
 */

ALTER TABLE public.dokument
    ADD COLUMN bekreftelses_url varchar(255);
