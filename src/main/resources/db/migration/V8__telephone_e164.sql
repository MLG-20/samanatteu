-- Les numéros de téléphone passent en E.164 (+221771234566).
--
-- Pourquoi : un numéro en format local est ambigu entre pays (771234566 existe au
-- Sénégal, en Côte d'Ivoire, au Mali) — en colonne UNIQUE, deux clients de pays
-- différents se marchent dessus. L'E.164 est international par nature, et c'est
-- exactement ce qu'exigent les fournisseurs SMS.
--
-- Conséquence à connaître : le téléphone est l'IDENTITÉ du compte (le "subject" du
-- JWT). Après cette migration, les tokens déjà émis ne valident plus : leurs
-- numéros ne correspondent à aucun compte en base. Les utilisateurs se reconnectent.
--
-- L'indicatif est figé ici (+221) : c'est la valeur de samanatteu.pays-defaut au
-- moment de la bascule. Un jour, la plateforme servira plusieurs pays : le numéro
-- portera son propre indicatif, et cette migration n'aura plus lieu d'être.

-- GARDE-FOU : deux comptes dont le numéro local se transforme en un numéro déjà
-- présent (l'un enregistré en E.164, l'autre en format local) entreraient en
-- collision. On le dit AVANT de modifier quoi que ce soit, avec un message
-- clair ; sans ce garde-fou, l'UPDATE échouerait sur la contrainte UNIQUE —
-- moins explicite, et sans la liste des comptes en cause.
DO $$
DECLARE
    collisions text;
BEGIN
    WITH convertis AS (
        SELECT id, nom, prenom, telephone, '+221' || telephone AS nouveau
        FROM utilisateur
        WHERE telephone NOT LIKE '+%'
    )
    SELECT string_agg(DISTINCT ligne, ' | ')
    INTO collisions
    FROM (
        -- Le compte déjà en E.164...
        SELECT u.id || ' (' || u.nom || ' ' || u.prenom || ' : ' || u.telephone || ')' AS ligne
        FROM convertis c
        JOIN utilisateur u ON u.telephone = c.nouveau
        UNION
        -- ...et le compte en format local qui va tomber dessus.
        SELECT c.id || ' (' || c.nom || ' ' || c.prenom || ' : ' || c.telephone || ')' AS ligne
        FROM convertis c
        JOIN utilisateur u ON u.telephone = c.nouveau
    ) liste;

    IF collisions IS NOT NULL THEN
        RAISE EXCEPTION 'Ces comptes se marchent dessus une fois convertis en E.164 : % . '
            'Supprimez ou fusionnez-les avant de relancer la migration.', collisions;
    END IF;
END $$;

UPDATE utilisateur
SET telephone = '+221' || telephone
WHERE telephone NOT LIKE '+%';

-- Les invitations individuelles gardent le numéro de la personne invitée.
-- Colonne nullable : seules les lignes renseignées sont converties.
UPDATE invitation
SET telephone = '+221' || telephone
WHERE telephone IS NOT NULL AND telephone NOT LIKE '+%';
