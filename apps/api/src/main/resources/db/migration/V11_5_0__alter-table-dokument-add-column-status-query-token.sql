-- Table: dokument

/*
ALTER TABLE public.dokument
    DROP COLUMN status_query_token;
 */

ALTER TABLE public.dokument
    ADD COLUMN status_query_token varchar(255);