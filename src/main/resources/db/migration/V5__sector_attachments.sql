    CREATE TABLE sector(
        id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
        name VARCHAR(100) NOT NULL UNIQUE,
        is_active BOOLEAN NOT NULL DEFAULT TRUE
    );

ALTER TABLE incident_notification DROP COLUMN incident_sector;

ALTER TABLE incident_notification
    ADD COLUMN sector_id INT NOT NULL,
    ADD CONSTRAINT fk_incident_notification_sector
        FOREIGN KEY (sector_id) REFERENCES sector(id);

CREATE INDEX idx_incident_notification_sector ON incident_notification(sector_id);
CREATE INDEX idx_incident_notification_event_date ON incident_notification(event_date);

ALTER TABLE incident_product_defect DROP COLUMN photo_sent_edoc;

CREATE TABLE incident_attachment(
  id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    notification_id INT NOT NULL REFERENCES incident_notification(id) ON DELETE CASCADE,
    original_name VARCHAR(255) NOT NULL,
    stored_path VARCHAR(255) NOT NULL UNIQUE,
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_incident_attachment_notification ON incident_attachment(notification_id);

ALTER TABLE incident_type ADD COLUMN code VARCHAR(30) UNIQUE;

UPDATE incident_type SET code = 'MEDICATION_ERROR' WHERE name ILIKE 'Falha / erro de medica%';
UPDATE incident_type SET code = 'PHLEBITIS'         WHERE name = 'Flebite';
UPDATE incident_type SET code = 'OTHER'             WHERE name = 'Outros';
UPDATE incident_type SET code = 'TECHNOVIGILANCE'   WHERE name ILIKE '%Tecnovigil%';
UPDATE incident_type SET code = 'PHARMACOVIGILANCE' WHERE name ILIKE '%Farmacovigil%';

DO $$
BEGIN
    IF (SELECT COUNT(*) FROM incident_type WHERE code IS NOT NULL) <> 5 THEN
        RAISE EXCEPTION 'V5: nao foi possivel identificar os 5 tipos de incidente especiais pelo nome.';
    END IF;
END $$;