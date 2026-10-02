-- 1. Canal d'envoi : SMS ou EMAIL
ALTER TABLE notification ADD COLUMN canal varchar(10) NOT NULL;

-- 2. Une notification a toujours un destinataire, un type, un message, un statut et une date
ALTER TABLE notification ALTER COLUMN destinataire_id SET NOT NULL;
ALTER TABLE notification ALTER COLUMN type SET NOT NULL;
ALTER TABLE notification ALTER COLUMN message SET NOT NULL;
ALTER TABLE notification ALTER COLUMN statut SET NOT NULL;
ALTER TABLE notification ALTER COLUMN created_at SET NOT NULL;
