-- Table: farskapserklaering

/*
    ALTER TABLE public.farskapserklaering
         DROP COLUMN deaktivert;
 */

ALTER TABLE public.farskapserklaering
    ADD COLUMN deaktivert timestamp without time zone;