-- Table: farskapserklaering

/*
ALTER TABLE public.farskapserklaering
    DROP COLUMN far_bor_sammen_med_mor
 */

ALTER TABLE public.farskapserklaering
    ADD COLUMN far_bor_sammen_med_mor boolean;
