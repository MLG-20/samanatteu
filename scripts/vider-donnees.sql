-- Vide toutes les DONNÉES (comptes, tontines, argent, notifications…) avant un
-- nouveau seed de démonstration. Garde ce qui est un réglage de la plateforme :
-- les formules (formule), les paramètres du site (parametre_site) et
-- l'historique Flyway (flyway_schema_history).
--
-- IRRÉVERSIBLE : faire d'abord une sauvegarde (pg_dump).
--   psql -h localhost -U samanatteu_user -d samanatteu -f scripts/vider-donnees.sql
--
-- RESTART IDENTITY : les id repartent à 1. CASCADE : les tables qui pointent
-- vers celles-ci sont vidées aussi (aucune ligne orpheline).
TRUNCATE TABLE
    utilisateur,
    abonnement,
    tontine,
    participation,
    cycle,
    cotisation,
    tirage,
    pret,
    echeance_pret,
    transaction,
    notification,
    invitation,
    import_membres,
    code_reinitialisation,
    verification_email,
    message_contact
RESTART IDENTITY CASCADE;
