CREATE TABLE roles (
    id_rol BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre_rol VARCHAR(50) NOT NULL UNIQUE
);

INSERT INTO roles (id_rol, nombre_rol) VALUES
(1, 'Observador'),
(2, 'Administrador');

CREATE TABLE usuarios (
    id_usuario BIGINT AUTO_INCREMENT PRIMARY KEY,
    rut VARCHAR(20) NOT NULL UNIQUE,
    contrasena VARCHAR(255) NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    correo VARCHAR(100) NOT NULL,
    telefono VARCHAR(20) NOT NULL,
    pregunta_seguridad VARCHAR(255) NOT NULL,
    respuesta_seguridad VARCHAR(255) NOT NULL,
    id_rol BIGINT NOT NULL DEFAULT 1,
    nombre_rol VARCHAR(50) NOT NULL DEFAULT 'Observador',
    avatar LONGBLOB,
    primer_ingreso TINYINT(1) NOT NULL DEFAULT 1,
    activo TINYINT(1) NOT NULL DEFAULT 1
);