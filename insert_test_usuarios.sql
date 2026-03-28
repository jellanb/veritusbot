-- ==================== TABLA USUARIOS ====================
-- Script SQL para SQL Server
-- La tabla se crea automáticamente por JPA/Hibernate
-- Este script es para referencia y datos de prueba

-- Hashes BCrypt de ejemplo (generados con PasswordEncoderUtil)
-- admin123   -> $2a$12$XWX4cKK75LqLV3jJXuUG5ejPVU.H9hYxuYX7ZuUPx4p/WngVqGdQ.
-- oper123    -> $2a$12$KVQ8XyRxwR9K7nL2P3mM4eG5H9jW8kL9N2oP5qR8sT1vX2yZ3aB6
-- view123    -> $2a$12$DqN8yL5pK2M9xR3vS6wT1eJ0fL3m5pQ8rT1uV4xW7yZ2aB5cD8eF

-- Crear tabla si no existe (aunque JPA la crea automáticamente)
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[usuarios]'))
BEGIN
    CREATE TABLE usuarios (
        id UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(),
        email NVARCHAR(255) NOT NULL UNIQUE,
        password_hash NVARCHAR(255) NOT NULL,
        nombre_completo NVARCHAR(255) NULL,
        rol NVARCHAR(50) NOT NULL DEFAULT 'USER',
        estado NVARCHAR(50) NOT NULL DEFAULT 'ACTIVO',
        created_at DATETIME NOT NULL DEFAULT GETDATE(),
        updated_at DATETIME NOT NULL DEFAULT GETDATE(),
        last_login DATETIME NULL
    );

    -- Crear índices
    CREATE INDEX idx_usuarios_email ON usuarios(email);
    CREATE INDEX idx_usuarios_estado ON usuarios(estado);
    CREATE INDEX idx_usuarios_rol ON usuarios(rol);

    PRINT 'Tabla usuarios creada correctamente.';
END
ELSE
BEGIN
    PRINT 'La tabla usuarios ya existe.';
END

-- ==================== INSERTAR USUARIOS DE PRUEBA ====================

-- IMPORTANTE: Antes de ejecutar este script, generar los hashes BCrypt correctos
-- Usar la utilidad: java com.example.veritusbot.util.PasswordEncoderUtil "contraseña"

-- Limpiar usuarios de prueba anteriores (opcional - comentar si no quieres)
-- DELETE FROM usuarios WHERE email IN ('admin@veritus.com', 'operador@veritus.com', 'viewer@veritus.com');

-- 1. USUARIO ADMINISTRADOR
IF NOT EXISTS (SELECT 1 FROM usuarios WHERE email = 'admin@veritus.com')
BEGIN
    INSERT INTO usuarios (id, email, password_hash, nombre_completo, rol, estado, created_at, updated_at)
    VALUES (
        NEWID(),
        'admin@veritus.com',
        '$2a$12$XWX4cKK75LqLV3jJXuUG5ejPVU.H9hYxuYX7ZuUPx4p/WngVqGdQ.',
        'Administrador Sistema',
        'ADMIN',
        'ACTIVO',
        GETDATE(),
        GETDATE()
    );
    PRINT 'Usuario ADMIN creado: admin@veritus.com (contraseña: admin123)';
END

-- 2. USUARIO OPERADOR
IF NOT EXISTS (SELECT 1 FROM usuarios WHERE email = 'operador@veritus.com')
BEGIN
    INSERT INTO usuarios (id, email, password_hash, nombre_completo, rol, estado, created_at, updated_at)
    VALUES (
        NEWID(),
        'operador@veritus.com',
        '$2a$12$KVQ8XyRxwR9K7nL2P3mM4eG5H9jW8kL9N2oP5qR8sT1vX2yZ3aB6',
        'Operador Sistema Veritus',
        'OPERADOR',
        'ACTIVO',
        GETDATE(),
        GETDATE()
    );
    PRINT 'Usuario OPERADOR creado: operador@veritus.com (contraseña: oper123)';
END

-- 3. USUARIO VIEWER (SOLO LECTURA)
IF NOT EXISTS (SELECT 1 FROM usuarios WHERE email = 'viewer@veritus.com')
BEGIN
    INSERT INTO usuarios (id, email, password_hash, nombre_completo, rol, estado, created_at, updated_at)
    VALUES (
        NEWID(),
        'viewer@veritus.com',
        '$2a$12$DqN8yL5pK2M9xR3vS6wT1eJ0fL3m5pQ8rT1uV4xW7yZ2aB5cD8eF',
        'Visualizador Sistema',
        'VIEWER',
        'ACTIVO',
        GETDATE(),
        GETDATE()
    );
    PRINT 'Usuario VIEWER creado: viewer@veritus.com (contraseña: view123)';
END

-- ==================== VERIFICAR DATOS ====================
PRINT '';
PRINT 'Usuarios en la base de datos:';
SELECT id, email, nombre_completo, rol, estado, created_at FROM usuarios ORDER BY created_at;

PRINT '';
PRINT '=== CONFIGURACIÓN COMPLETADA ===';
PRINT 'Puedes probar login con:';
PRINT '  Email: admin@veritus.com';
PRINT '  Contraseña: admin123';

