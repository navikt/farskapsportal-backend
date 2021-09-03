-- Table: dokument

/*
ALTER TABLE public.dokument
    RENAME COLUMN navn TO dokumentnavn;
ALTER TABLE public.dokument
    RENAME COLUMN status_url TO dokument_status_url;
 */

ALTER TABLE public.dokument
    RENAME COLUMN dokumentnavn TO navn;
ALTER TABLE public.dokument
    RENAME COLUMN dokument_status_url TO status_url;
