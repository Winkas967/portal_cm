INSERT INTO users(username, password_hash, role_id)
VALUES ('admin', '$2b$10$OU4yP40ioTqEd1N/4vv3le9o0ST1LA8pFvzNjoxvkqsRMMNkX98f6', 3);


-- Carga inicial
INSERT INTO incident_type (name) VALUES
    ('Queda'),('Falha da identificação do paciente'),('Broncoaspiração'),
    ('Falha / erro de medicação'),('Erro/falha em cirurgia'),
    ('Lesão de pele relacionada a adesivos hospitalares'),('Erro/falha em dieta'),
    ('Dermatite associada à incontinência (DAI)'),('Tempo expirado para testes rápidos'),
    ('Falhas estruturais'),('Complicação no acesso venoso periférico'),('Flebite'),
    ('Saída acidental de dreno'),('Saída acidental de sonda nasoenteral/gástrica'),
    ('Falhas no cuidado/proteção do paciente'),('Extubação acidental'),('Oxigenoterapia'),
    ('Lesão por contenção física'),('Fuga de paciente'),('Óbito por demora no atendimento'),
    ('Perda da amostra biológica insubstituível'),('Lesão por pressão'),
    ('Falha equipamento hospitalar – Tecnovigilância'),
    ('Defeito qualidade medicação – Farmacovigilância'),
    ('Falha na proteção do paciente contra infecções hospitalares'),('Outros');

INSERT INTO possible_cause (name, description) VALUES
    ('Falta de conhecimento','Não sabe o que fazer'),
    ('Regras','Sabe o que fazer, mas não aplica as regras ou as aplica incorretamente'),
    ('Falta de dados','Informação insuficiente ou parcial / falhas de comunicação'),
    ('Especificações insuficientes','Processo não claramente definido'),
    ('Ações automáticas','Processo precisa ser revisto para averiguar novas variáveis'),
    ('Distração',NULL),('Esquecimento',NULL),('Outros',NULL);