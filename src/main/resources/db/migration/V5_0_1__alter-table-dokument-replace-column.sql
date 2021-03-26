-- Table: dokument

/*
    ALTER TABLE public.dokument
         ADD COLUMN innhold bytea,
         DROP COLUMN dokumentinnhold;
 */

ALTER TABLE public.dokument
    DROP COLUMN innhold,
    ADD COLUMN dokumentinnhold_id integer,
    ADD CONSTRAINT fk_dokumentinnhold FOREIGN KEY (dokumentinnhold_id)
        REFERENCES dokumentinnhold (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE CASCADE;
