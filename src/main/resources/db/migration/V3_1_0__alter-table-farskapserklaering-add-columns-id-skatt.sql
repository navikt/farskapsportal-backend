-- Table: farskapserklaering

/*
    ALTER TABLE public.farskapserklaering
         DROP COLUMN meldingsid_skatt,
         DROP COLUMN sendt_til_skatt;
 */

ALTER TABLE public.farskapserklaering
    ADD COLUMN meldingsid_skatt varchar(1000),
    ADD COLUMN sendt_til_skatt timestamp without time zone;
