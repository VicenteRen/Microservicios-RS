CREATE TABLE registros_observacion (
    id_registro BIGINT AUTO_INCREMENT PRIMARY KEY,
    clasificacion VARCHAR(50) NOT NULL,
    rango_etario VARCHAR(50) NOT NULL,
    compania VARCHAR(50) NOT NULL,
    compras_otra_tienda TINYINT(1) NOT NULL DEFAULT 0,
    acceso_destino VARCHAR(100) NULL,
    fecha_hora VARCHAR(30) NOT NULL,
    id_usuario BIGINT NOT NULL,
    nombre_observador VARCHAR(150) NOT NULL,
    activo TINYINT(1) NOT NULL DEFAULT 1
);
