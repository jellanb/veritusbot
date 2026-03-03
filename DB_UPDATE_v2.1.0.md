# 🗄️ Actualización de Base de Datos - v2.1.0

## ⚠️ IMPORTANTE

Si ya tienes la BD creada, **debes ejecutar estas instrucciones** para agregar el nuevo campo necesario para la v2.1.0.

## 📋 Script SQL

### Opción 1: Agregar Campo (Recomendado)

```sql
-- Conectar a la base de datos
USE veritus;

-- Agregar la nueva columna a la tabla personas_procesadas
ALTER TABLE personas_procesadas 
ADD tribunal_principal_procesado BIT NULL;

-- Verificar que se creó correctamente
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'personas_procesadas'
ORDER BY ORDINAL_POSITION;
```

**Resultado esperado:**
```
COLUMN_NAME                    DATA_TYPE  IS_NULLABLE
====================================================
id                             int        NO
primer_nombre                  nvarchar   YES
segundo_nombre                 nvarchar   YES
apellido_paterno               nvarchar   YES
apellido_materno               nvarchar   YES
rut                            nvarchar   YES
procesado                      bit        YES
creada                         datetime   YES
fecha_procesada                datetime   YES
tribunal_principal_procesado   bit        YES
```

### Opción 2: Recrear Tabla Completa (Limpia)

Si necesitas tabla completamente nueva sin registros:

```sql
USE veritus;

-- Eliminar tabla anterior
DROP TABLE IF EXISTS personas_procesadas;

-- Crear tabla con todos los campos incluyendo el nuevo
CREATE TABLE personas_procesadas (
    id INT IDENTITY(1,1) PRIMARY KEY,
    primer_nombre NVARCHAR(100) NULL,
    segundo_nombre NVARCHAR(100) NULL,
    apellido_paterno NVARCHAR(100) NULL,
    apellido_materno NVARCHAR(100) NULL,
    rut NVARCHAR(20) NULL,
    procesado BIT NULL,
    creada DATETIME NULL,
    fecha_procesada DATETIME NULL,
    tribunal_principal_procesado BIT NULL
);

-- Crear índices para búsquedas rápidas
CREATE INDEX idx_nombres_apellidos 
ON personas_procesadas (primer_nombre, apellido_paterno);

CREATE INDEX idx_procesado 
ON personas_procesadas (procesado);

CREATE INDEX idx_tribunal_principal 
ON personas_procesadas (tribunal_principal_procesado);
```

## 🔍 Verificación

### Verificar que todo está correcto

```sql
-- 1. Verificar que la tabla existe y tiene los campos
SELECT * FROM personas_procesadas;

-- 2. Verificar estructura
EXEC sp_help 'personas_procesadas';

-- 3. Verificar que el nuevo campo permite NULL
SELECT 
    COLUMN_NAME,
    IS_NULLABLE,
    DATA_TYPE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'personas_procesadas'
  AND COLUMN_NAME = 'tribunal_principal_procesado';

-- Resultado esperado: tribunal_principal_procesado, YES, bit
```

## 📊 Valores del Campo `tribunal_principal_procesado`

| Valor | Significado | Estado |
|-------|-----------|--------|
| `NULL` | No inicializado | Persona nueva o antigua |
| `0` | FALSE | Fase 1 no completada |
| `1` | TRUE | Fase 1 completada, puede proceder a Fase 2 |

## 🚀 Después de Actualizar

Una vez ejecutado el script SQL:

### 1. Recompilar la aplicación
```bash
cd /Users/jellan/Documents/git/veritusbot
mvn clean package -DskipTests
```

### 2. Reiniciar la aplicación
```bash
java -jar target/veritusbot-0.0.1-SNAPSHOT.jar
```

### 3. Verificar en logs
```
INICIANDO BÚSQUEDA DE PERSONAS DESDE EXCEL
FASE 1: PROCESANDO TRIBUNALES DE SANTIAGO (1º-30º)
FASE 2: PROCESANDO OTROS TRIBUNALES (EXCLUYENDO SANTIAGO)
✓ Tribunales de Santiago procesados. tribunal_principal_procesado = true
```

## ⚠️ Notas Importantes

### Preservar Datos Existentes
- **Opción 1 (recomendada):** Solo agrega el campo, mantiene todos los datos
- **Opción 2:** Elimina la tabla, pierde datos anteriores

### Backup
Es recomendable hacer backup antes de ejecutar cambios en BD:

```bash
# SQL Server con Docker/Podman
podman exec sqlserver /opt/mssql-tools/bin/sqlcmd \
  -S localhost,1433 -U sa -P SqlServer2026Strong \
  -Q "BACKUP DATABASE veritus TO DISK = '/var/opt/mssql/backup/veritus_backup.bak'"
```

### Rollback si hay problema
Si necesitas revertir (solo aplica a Opción 1):

```sql
ALTER TABLE personas_procesadas 
DROP COLUMN tribunal_principal_procesado;
```

## 🔄 Proceso Actualización Paso a Paso

### Para usuarios con BD ya creada:

1. **Detener la aplicación** (si está corriendo)
   ```bash
   # Presionar Ctrl+C en la terminal
   ```

2. **Conectar a SQL Server**
   ```bash
   # Usando DataGrip, Azure Data Studio, SQL Server Management Studio, etc.
   # O por línea de comandos:
   podman exec -it sqlserver /opt/mssql-tools/bin/sqlcmd \
     -S localhost,1433 -U sa -P SqlServer2026Strong
   ```

3. **Ejecutar el script SQL (Opción 1 recomendada)**
   ```sql
   USE veritus;
   ALTER TABLE personas_procesadas 
   ADD tribunal_principal_procesado BIT NULL;
   ```

4. **Verificar**
   ```sql
   SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE
   FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_NAME = 'personas_procesadas'
   ORDER BY ORDINAL_POSITION;
   ```

5. **Recompilar**
   ```bash
   cd /Users/jellan/Documents/git/veritusbot
   mvn clean package -DskipTests
   ```

6. **Reiniciar la aplicación**
   ```bash
   java -jar target/veritusbot-0.0.1-SNAPSHOT.jar
   ```

## ✅ Checklist Pre-Actualización

- [ ] BD SQL Server está corriendo
- [ ] Tengo acceso a la BD (usuario/contraseña funcionando)
- [ ] Tengo backup (opcional pero recomendado)
- [ ] Aplicación no está corriendo
- [ ] Compilación anterior es exitosa

## ❓ Preguntas Frecuentes

### P: ¿Pierdo datos si ejecuto Opción 2?
R: Sí, Opción 2 elimina la tabla. Usa Opción 1 si quieres mantener datos.

### P: ¿Es obligatorio actualizar?
R: Sí, la v2.1.0 requiere este campo. Sin él, la app fallará.

### P: ¿Qué pasa si olvido actualizar?
R: La aplicación mostrará un error al intentar insertar personas procesadas.

### P: ¿Puedo volver a la versión anterior?
R: Sí, revertiendo el script SQL (DROP COLUMN) y usando JAR anterior.

---

**Última actualización:** 2 Marzo 2026  
**Versión aplicable:** v2.1.0+


