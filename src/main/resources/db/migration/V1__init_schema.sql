CREATE TABLE role(
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    role VARCHAR(50) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE users(
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role_id INT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT fk_role_user FOREIGN KEY (role_id) REFERENCES role(id)
);

-- Catálogos
CREATE TABLE incident_type (
    id     INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name   VARCHAR(150) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE possible_cause (
    id          INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(150) NOT NULL UNIQUE,
    description VARCHAR(255),
    active      BOOLEAN NOT NULL DEFAULT TRUE
);

-- Ficha principal
CREATE TABLE incident_notification (
   id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
   incident_sector   VARCHAR(100) NOT NULL,
   notifier          VARCHAR(150) NOT NULL,
   notification_date TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
   event_date        TIMESTAMPTZ  NOT NULL,

-- Seção 1: detalhes de alguns tipos
   medication_error_stage VARCHAR(20)
       CHECK (medication_error_stage IN ('prescricao','dispensacao','administracao')),
   phlebitis_type VARCHAR(20)
       CHECK (phlebitis_type IN ('quimica','mecanica','infecciosa')),
   other_incident_description TEXT,

-- Seção 3
   nonconformity TEXT,

-- Seção 4: paciente
   patient_name          VARCHAR(150) NOT NULL,
   patient_birth_date    DATE,
   patient_color         VARCHAR(30),
   visit_reason          TEXT,
   medical_record_number VARCHAR(30),
   attendance_number     VARCHAR(30),
   ward_bed              VARCHAR(50),

   event_description TEXT NOT NULL,
   immediate_actions TEXT,

-- Reservado ao NSP
   harm_level VARCHAR(30) CHECK (harm_level IN (
        'sem_agravo','agravo_leve','agravo_moderado','agravo_grave',
        'invalidez_temporaria','invalidez_permanente','obito')),
   pressure_injury_grade   SMALLINT CHECK (pressure_injury_grade BETWEEN 1 AND 4),
   improvement_suggestions TEXT,
   other_cause_description TEXT,

   created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
   CHECK (event_date <= notification_date)
);

-- Seção 1: vários tipos marcados por ficha
CREATE TABLE incident_notification_type (
    notification_id  INT NOT NULL REFERENCES incident_notification(id) ON DELETE CASCADE,
    incident_type_id INT NOT NULL REFERENCES incident_type(id),
    PRIMARY KEY (notification_id, incident_type_id)
);

-- Seção 2: farmaco/tecnovigilância (0 ou 1 por ficha)
CREATE TABLE incident_product_defect (
     notification_id INT PRIMARY KEY REFERENCES incident_notification(id) ON DELETE CASCADE,
     kind VARCHAR(20) NOT NULL CHECK (kind IN ('medicamento','equipamento')),
     product_name  VARCHAR(150),
     manufacturer  VARCHAR(150),
     batch         VARCHAR(50),
     expiry_date   DATE,
     equipment     VARCHAR(150),
     serial_number VARCHAR(80),
     last_preventive_maintenance DATE,
     asset_number  VARCHAR(50),
     photo_sent_edoc BOOLEAN NOT NULL DEFAULT FALSE
);

-- Possíveis causas (NSP)
CREATE TABLE incident_notification_cause (
     notification_id INT NOT NULL REFERENCES incident_notification(id) ON DELETE CASCADE,
     cause_id        INT NOT NULL REFERENCES possible_cause(id),
     PRIMARY KEY (notification_id, cause_id)
);

