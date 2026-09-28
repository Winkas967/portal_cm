-- O notificador passa a ser o usuário logado que registrou a ficha,
-- em vez de um nome digitado livremente.
ALTER TABLE incident_notification DROP COLUMN notifier;

ALTER TABLE incident_notification
    ADD COLUMN notifier_id INT NOT NULL,
    ADD CONSTRAINT fk_incident_notification_notifier
        FOREIGN KEY (notifier_id) REFERENCES users(id);

CREATE INDEX idx_incident_notification_notifier ON incident_notification(notifier_id);
