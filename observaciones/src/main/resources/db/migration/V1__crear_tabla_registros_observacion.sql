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

-- Datos de prueba iniciales para probar con Postman de inmediato
INSERT INTO registros_observacion 
(clasificacion, rango_etario, compania, compras_otra_tienda, acceso_destino, fecha_hora, id_usuario, nombre_observador, activo) 
VALUES 
('Tránsito', '26 - 40 años', 'Solo', 0, 'A1 - Acceso Norte', '15/11/2026 10:30', 1, 'Pepito Lospalote', 1),
('Visita', '15 - 25 años', 'En pareja', 1, 'A2 - Acceso Sur', '15/11/2026 11:15', 1, 'Pepito Lospalote', 1);