-- Table: farskapserklaering

/*
    ALTER TABLE public.farskapserklaering
         DROP COLUMN mor_bor_sammen_med_far,
         DROP COLUMN far_bor_sammen_med_mor;
 */

ALTER TABLE public.farskapserklaering
    ADD COLUMN mor_bor_sammen_med_far "char",
    ADD COLUMN far_bor_sammen_med_mor "char";
