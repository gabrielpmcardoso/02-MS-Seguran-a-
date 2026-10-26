-- username é o identificador (userid) solicitado no enunciado.
-- VARCHAR(255) comporta o hash BCrypt; a senha nunca é armazenada em texto puro.
CREATE TABLE IF NOT EXISTS usuarios (
    username VARCHAR(100) PRIMARY KEY,
    senha VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE TABLE IF NOT EXISTS login_attempts (
    username VARCHAR(100) PRIMARY KEY REFERENCES usuarios(username),
    failed_attempts INT NOT NULL DEFAULT 0,
    locked BOOLEAN NOT NULL DEFAULT FALSE,
    last_failed_at TIMESTAMP NULL,
    locked_at TIMESTAMP NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS authorities (
    username VARCHAR(100) NOT NULL REFERENCES usuarios(username),
    authority VARCHAR(50) NOT NULL,
    PRIMARY KEY (username, authority)
);
-- A tabela original não armazena históricos acumulados de sucesso/erro.
CREATE TABLE IF NOT EXISTS auth_metrics (
    id INT PRIMARY KEY CHECK (id = 1),
    valid_logins BIGINT NOT NULL DEFAULT 0,
    invalid_logins BIGINT NOT NULL DEFAULT 0
);
INSERT INTO auth_metrics (id) SELECT 1 WHERE NOT EXISTS (SELECT 1 FROM auth_metrics WHERE id = 1);
