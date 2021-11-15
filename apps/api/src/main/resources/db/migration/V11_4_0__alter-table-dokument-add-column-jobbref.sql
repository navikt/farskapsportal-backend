-- Table: dokument

/*
ALTER TABLE public.dokument
    DROP COLUMN jobbref;
 */

ALTER TABLE public.dokument
    ADD COLUMN jobbref varchar(255);