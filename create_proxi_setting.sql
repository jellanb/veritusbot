-- ==================== TABLA PROXI_SETTING ====================
-- Script SQL para SQL Server
-- La tabla se crea automáticamente por JPA/Hibernate
-- Este script es para referencia y migración manual si fuera necesario

IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[proxi_setting]'))
BEGIN
    CREATE TABLE proxi_setting (
        id       BIGINT IDENTITY(1,1) PRIMARY KEY,
        server   NVARCHAR(255) NOT NULL,
        username NVARCHAR(100) NULL,
        password NVARCHAR(100) NULL,
        activo   BIT NOT NULL DEFAULT 1,
        orden    INT NOT NULL DEFAULT 0
    );

    CREATE INDEX idx_proxi_setting_activo ON proxi_setting(activo);
    CREATE INDEX idx_proxi_setting_orden  ON proxi_setting(orden);

    PRINT 'Tabla proxi_setting creada correctamente.';
END
ELSE
BEGIN
    PRINT 'La tabla proxi_setting ya existe.';
END
GO

-- ==================== MIGRAR PROXIES DESDE APPLICATION.PROPERTIES ====================
-- Si tenías proxies configurados en el archivo properties, insértalos aquí.
-- Descomenta y ajusta los valores según tu configuración anterior.

-- EJEMPLO (ajusta server, username y password con tus valores reales):
-- INSERT INTO proxi_setting (server, username, password, activo, orden) VALUES ('http://TU_PROXY_1:PUERTO', 'usuario1', 'clave1', 1, 1);
-- INSERT INTO proxi_setting (server, username, password, activo, orden) VALUES ('http://TU_PROXY_2:PUERTO', 'usuario2', 'clave2', 1, 2);
-- INSERT INTO proxi_setting (server, username, password, activo, orden) VALUES ('http://TU_PROXY_3:PUERTO', 'usuario3', 'clave3', 1, 3);
-- INSERT INTO proxi_setting (server, username, password, activo, orden) VALUES ('http://TU_PROXY_4:PUERTO', 'usuario4', 'clave4', 1, 4);
-- INSERT INTO proxi_setting (server, username, password, activo, orden) VALUES ('http://TU_PROXY_5:PUERTO', 'usuario5', 'clave5', 1, 5);
-- INSERT INTO proxi_setting (server, username, password, activo, orden) VALUES ('http://TU_PROXY_6:PUERTO', 'usuario6', 'clave6', 1, 6);
GO

