ALTER TABLE incident_notification
    ADD COLUMN code VARCHAR(255);

UPDATE incident_notification
SET code = 'NAO_INFORMADO'
WHERE code IS NULL;

ALTER TABLE incident_notification
    ALTER COLUMN code SET NOT NULL;