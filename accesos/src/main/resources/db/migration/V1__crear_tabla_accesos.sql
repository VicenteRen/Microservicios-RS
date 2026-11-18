CREATE TABLE accesos (
    id_acceso BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo VARCHAR(10) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL,
    activo TINYINT(1) NOT NULL DEFAULT 1
);

INSERT INTO accesos (codigo, nombre, activo) VALUES
('A1', 'Acceso Norte', 1),
('A2', 'Acceso Sur', 1),
('A3', 'Estacionamiento', 1),
('A4', 'Acceso Poniente', 1);