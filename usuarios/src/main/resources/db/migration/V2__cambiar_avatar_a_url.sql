ALTER TABLE usuarios
    DROP COLUMN avatar,
    ADD COLUMN avatar_url VARCHAR(500) NULL AFTER nombre_rol;