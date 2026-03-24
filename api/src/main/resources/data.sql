INSERT INTO activity_types (name)
SELECT 'Yoga'
WHERE NOT EXISTS (SELECT 1 FROM activity_types WHERE name = 'Yoga');