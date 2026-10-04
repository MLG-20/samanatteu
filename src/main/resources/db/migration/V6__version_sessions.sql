-- Numéro de version des sessions d'un compte. Chaque token porte le numéro du
-- moment où il a été fabriqué ; se déconnecter ou changer de mot de passe ajoute 1,
-- ce qui rend tous les tokens précédents inutilisables.
ALTER TABLE utilisateur ADD COLUMN version_sessions integer NOT NULL DEFAULT 0;
