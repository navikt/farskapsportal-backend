-- Table: dokument

-- DROP TABLE dokument, then rerun V1_0_2

ALTER TABLE public.dokument
    DROP COLUMN redirect_url_far,
    DROP COLUMN redirect_url_mor,
    DROP COLUMN signert_av_mor,
    DROP COLUMN signert_av_far,
    ADD COLUMN signeringsinformasjon_mor_id integer,
    ADD COLUMN signeringsinformasjon_far_id integer,
    ADD CONSTRAINT fk_singeringsinformasjon_mor_id FOREIGN KEY (signeringsinformasjon_mor_id)
    REFERENCES signeringsinformasjon (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    ADD CONSTRAINT fk_singeringsinformasjon_far_id FOREIGN KEY (signeringsinformasjon_far_id)
    REFERENCES signeringsinformasjon (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION;
