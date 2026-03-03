# ⚡ Guía de Instalación Rápida v2.1.0

## 5 Pasos para Activar la Nueva Versión

### Paso 1: Actualizar Base de Datos (2 minutos)

```bash
# Conectar a SQL Server
podman exec -it sqlserver /opt/mssql-tools/bin/sqlcmd \
  -S localhost,1433 -U sa -P SqlServer2026Strong
```

En la consola SQL:
```sql
USE veritus;
ALTER TABLE personas_procesadas 
ADD tribunal_principal_procesado BIT NULL;
GO
```

Verificar:
```sql
SELECT COLUMN_NAME, DATA_TYPE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'personas_procesadas'
  AND COLUMN_NAME = 'tribunal_principal_procesado';
GO
```

Debe aparecer: `tribunal_principal_procesado | bit`

### Paso 2: Detener Aplicación Anterior (1 minuto)

```bash
# Si está corriendo, presionar Ctrl+C
# O matar el proceso:
lsof -i :8083 | grep java | awk '{print $2}' | xargs kill -9
```

### Paso 3: Compilar Nueva Versión (20 segundos)

```bash
cd /Users/jellan/Documents/git/veritusbot
mvn clean package -DskipTests 2>&1 | tail -20
```

Debe terminar con:
```
BUILD SUCCESS
[INFO] Building jar: .../target/veritusbot-0.0.1-SNAPSHOT.jar
```

### Paso 4: Iniciar Aplicación (10 segundos)

```bash
java -jar target/veritusbot-0.0.1-SNAPSHOT.jar
```

Esperar a que aparezca:
```
✓ Conexión a BD: OK
✓ API disponible en: http://localhost:8083
```

### Paso 5: Validar Funcionamiento (30 segundos)

En otra terminal:
```bash
# Test rápido
curl "http://localhost:8083/api/test"

# Resultado esperado: "✓ API funcionando correctamente"
```

---

## ✨ Características Ahora Activas

✅ **Búsqueda en Dos Fases**
- Fase 1: Tribunales de Santiago (1º-30º)
- Fase 2: Todos los demás tribunales

✅ **Campo de Auditoría**
- `tribunal_principal_procesado = TRUE` cuando Fase 1 completada

✅ **Logs Mejorados**
- Muestra claramente qué fase se está ejecutando
- Indica tribunales procesados y omitidos

---

## 🧪 Test Básico (1 minuto)

```bash
# Terminal 1: Aplicación corriendo
java -jar target/veritusbot-0.0.1-SNAPSHOT.jar

# Terminal 2: Ejecutar búsqueda
curl "http://localhost:8083/api/buscar-personas?archivo=personas.csv"
```

Revisar logs en Terminal 1:
```
FASE 1: PROCESANDO TRIBUNALES DE SANTIAGO (1º-30º)
✓ Tribunales de Santiago procesados. tribunal_principal_procesado = true

FASE 2: PROCESANDO OTROS TRIBUNALES (EXCLUYENDO SANTIAGO)
```

---

## 🔍 Verificar en BD Después (1 minuto)

```sql
USE veritus;

SELECT 
    primer_nombre,
    apellido_paterno,
    procesado,
    tribunal_principal_procesado,
    CONVERT(VARCHAR(19), fecha_procesada, 120) as fecha
FROM personas_procesadas
ORDER BY fecha_procesada DESC;
GO
```

Debe mostrar:
- `procesado = 1` (completado)
- `tribunal_principal_procesado = 1` (Fase 1 completada)
- `fecha_procesada = 2026-03-02 ...` (fecha actual)

---

## 📊 Tiempo Total: 5-10 minutos

| Paso | Tarea | Tiempo |
|------|-------|--------|
| 1 | Actualizar BD | 2 min |
| 2 | Detener app | 1 min |
| 3 | Compilar | 20 seg |
| 4 | Iniciar | 10 seg |
| 5 | Validar | 30 seg |
| **TOTAL** | | **4-5 min** |

---

## ⚠️ Si Algo Sale Mal

### Error: "Column 'tribunal_principal_procesado' not found"
```sql
-- Verificar que el campo existe
SELECT COLUMN_NAME 
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'personas_procesadas'
  AND COLUMN_NAME = 'tribunal_principal_procesado';

-- Si no aparece, ejecutar:
ALTER TABLE personas_procesadas 
ADD tribunal_principal_procesado BIT NULL;
```

### Error: "Port 8083 already in use"
```bash
# Matar proceso anterior
lsof -i :8083
kill -9 [PID]

# O usar otro puerto en application.properties
server.port=8084
```

### Error: "Cannot connect to BD"
```bash
# Verificar que SQL Server está corriendo
podman ps | grep sqlserver

# Si no aparece, iniciar
podman start sqlserver
sleep 20
```

---

## 📚 Documentación Completa

Consultar:
- `RESUMEN_EJECUTIVO_v2.1.0.md` - Overview de cambios
- `CHANGELOG.md` - Detalles técnicos
- `DB_UPDATE_v2.1.0.md` - Scripts SQL detallados
- `TESTING_v2.1.0.md` - Testing completo
- `README.md` - Documentación general

---

## ✅ Checklist de Activación

- [ ] BD actualizada con nuevo campo
- [ ] Compilación exitosa (BUILD SUCCESS)
- [ ] Aplicación inicia sin errores
- [ ] API responde en http://localhost:8083/api/test
- [ ] Logs muestran "FASE 1" y "FASE 2"
- [ ] Campo se actualiza en BD correctamente

---

## 🎯 Próximas Acciones

1. ✅ Instalar v2.1.0 (este documento)
2. 📝 Ejecutar pruebas (TESTING_v2.1.0.md)
3. 📊 Monitorear logs en producción
4. 🔄 Procesar próximo lote de personas

---

**¡Listo para usar en 5 minutos! 🚀**

---

**Versión:** 2.1.0  
**Fecha:** 2 Marzo 2026  
**Última revisión:** Hoy


