-- 1. Ancienne ligne de test invalide (sens 'ENTREE', montant négatif)
DELETE FROM transaction;

-- 2. Mode et référence externe de chaque paiement
ALTER TABLE transaction ADD COLUMN mode_paiement varchar(20);
ALTER TABLE transaction ADD COLUMN reference varchar(100);

-- 3. Colonnes obligatoires
ALTER TABLE transaction ALTER COLUMN type SET NOT NULL;
ALTER TABLE transaction ALTER COLUMN sens SET NOT NULL;
ALTER TABLE transaction ALTER COLUMN montant SET NOT NULL;

-- 4. Le sens porte la direction : le montant est toujours positif
ALTER TABLE transaction ADD CONSTRAINT transaction_montant_positif CHECK (montant > 0);
