CREATE TABLE usuarios (
                          id             BIGINT       NOT NULL AUTO_INCREMENT,
                          username       VARCHAR(50)  NOT NULL,
                          password       VARCHAR(100) NOT NULL,
                          nombres        VARCHAR(100) NOT NULL,
                          apellidos      VARCHAR(100) NOT NULL,
                          email          VARCHAR(120) NOT NULL,
                          rol            VARCHAR(20)  NOT NULL,
                          activo         BOOLEAN      NOT NULL DEFAULT TRUE,
                          fecha_creacion DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                          PRIMARY KEY (id),
                          CONSTRAINT uk_usuarios_username UNIQUE (username),
                          CONSTRAINT uk_usuarios_email UNIQUE (email)
);