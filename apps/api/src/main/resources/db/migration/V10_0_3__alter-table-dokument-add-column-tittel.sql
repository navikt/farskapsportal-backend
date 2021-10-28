-- Table: dokument

/*
ALTER TABLE public.dokument
    DROP COLUMN tittel
 */

ALTER TABLE public.dokument
    ADD COLUMN tittel varchar(255);
