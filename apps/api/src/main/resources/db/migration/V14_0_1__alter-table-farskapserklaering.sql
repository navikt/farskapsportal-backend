-- Table: farskapserklaering

/*** revert **

 ALTER TABLE public.farskapserklaering DROP COLUMN dokumenter_slettet;
 DELETE FROM flyway_schema_history WHERE version = '14.0.1';
 */

ALTER TABLE public.farskapserklaering ADD COLUMN dokumenter_slettet timestamp without time zone;