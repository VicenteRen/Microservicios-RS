
ALTER TABLE registros_observacion
    ADD COLUMN anulado_por BIGINT NULL,
    ADD COLUMN motivo_anulacion VARCHAR(255) NULL,
    ADD COLUMN fecha_anulacion DATETIME NULL;
