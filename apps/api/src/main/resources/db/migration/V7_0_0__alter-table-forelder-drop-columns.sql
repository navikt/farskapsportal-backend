-- Table: forelder

/*
    ALTER TABLE public.forelder
        ADD COLUMN fornavn varchar(255),
        ADD COLUMN mellomnavn varchar(255),
        ADD COLUMN etternavn varchar(255);
 */

ALTER TABLE public.forelder
    DROP COLUMN fornavn,
    DROP COLUMN mellomnavn,
    DROP COLUMN etternavn;
