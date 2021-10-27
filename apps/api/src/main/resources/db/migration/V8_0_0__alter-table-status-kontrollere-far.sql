-- Table: public.status_kontrollere_far

/*
    ALTER TABLE public.status_kontrollere_far
    RENAME COLUMN tidspunkt_siste_feilede_forsoek TO tidspunkt_for_nullstilling
 */

ALTER TABLE public.status_kontrollere_far
    RENAME COLUMN tidspunkt_siste_feilede_forsoek TO tidspunkt_for_nullstilling

