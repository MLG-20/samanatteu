-- 1. Anciennes lignes de test de l'ancien CRUD (token en double, sans type)
DELETE FROM invitation;

-- 2. Type d'invitation : lien de groupe ou invitation individuelle
ALTER TABLE invitation ADD COLUMN type varchar(20) NOT NULL;

-- 3. Le token identifie l'invitation : obligatoire et unique
ALTER TABLE invitation ALTER COLUMN token SET NOT NULL;
ALTER TABLE invitation ADD CONSTRAINT invitation_token_unique UNIQUE (token);

-- 4. Une invitation a toujours une tontine, une expiration et un statut
ALTER TABLE invitation ALTER COLUMN tontine_id SET NOT NULL;
ALTER TABLE invitation ALTER COLUMN expire_at SET NOT NULL;
ALTER TABLE invitation ALTER COLUMN statut SET NOT NULL;
