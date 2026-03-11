-- Migration : ajout des colonnes de profil utilisateur
-- À exécuter UNE SEULE FOIS sur la base de données existante.
-- Hibernate (ddl-auto=update) ajoutera automatiquement les colonnes
-- bio, phone, address, created_at si elles n'existent pas encore.

-- 1. Ajouter les colonnes nullable (si elles n'existent pas déjà)
ALTER TABLE users ADD COLUMN IF NOT EXISTS bio TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS phone VARCHAR(30);
ALTER TABLE users ADD COLUMN IF NOT EXISTS address VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS created_at DATE;

-- 2. Remplir created_at pour les utilisateurs existants avec CURRENT_DATE
UPDATE users SET created_at = CURRENT_DATE WHERE created_at IS NULL;
