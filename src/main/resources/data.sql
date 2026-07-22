INSERT INTO activity_types (name, is_deleted)
SELECT 'Autre', false
WHERE NOT EXISTS (
    SELECT 1 FROM activity_types WHERE LOWER(name) = 'autre' AND is_deleted = false
);
