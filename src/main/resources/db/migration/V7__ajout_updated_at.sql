-- V7 : ajoute updated_at aux 11 tables qui ne l'ont pas.
-- (tontine et utilisateur l'ont déjà depuis V1.)
-- Nullable exprès : les vieilles lignes restent à NULL,
-- remplies au prochain UPDATE via @PreUpdate.
-- Jamais de NOT NULL ici : ça bloquerait sur les données existantes.
ALTER TABLE public.cotisation ADD COLUMN updated_at timestamp without time zone;
ALTER TABLE public.cycle ADD COLUMN updated_at timestamp without time zone;
ALTER TABLE public.echeance_pret ADD COLUMN updated_at timestamp without time zone;
ALTER TABLE public.import_membres ADD COLUMN updated_at timestamp without time zone;
ALTER TABLE public.invitation ADD COLUMN updated_at timestamp without time zone;
ALTER TABLE public.notification ADD COLUMN updated_at timestamp without time zone;
ALTER TABLE public.participation ADD COLUMN updated_at timestamp without time zone;
ALTER TABLE public.pret ADD COLUMN updated_at timestamp without time zone;
ALTER TABLE public.tirage ADD COLUMN updated_at timestamp without time zone;
ALTER TABLE public.transaction ADD COLUMN updated_at timestamp without time zone;
ALTER TABLE code_reinitialisation ADD COLUMN updated_at timestamp without time zone;