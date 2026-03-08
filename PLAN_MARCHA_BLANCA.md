# 📋 Plan de Marcha Blanca - VERITUS BOT v2.1

**Fecha de Creación:** 8 de Marzo de 2026  
**Estado:** ACTIVO  
**Versión de Aplicación:** 2.1  
**Ambiente:** PRODUCCIÓN  

---

## 1. 🎯 OBJETIVO GENERAL

Desplegar y validar VERITUS BOT en ambiente de producción (Windows Server 2019+) con integración a SQL Server, garantizando disponibilidad 24/7 sin downtime y procesamiento de hasta 50,000 registros en 6 meses.

---

## 2. 📊 ESPECIFICACIONES DE INFRAESTRUCTURA PRODUCCIÓN

### 2.1 Servidor Destino
- **Propietario:** Escritorio Jurídico
- **Sistema Operativo:** Windows Server 2019 o posterior
- **Procesador:** Intel Xeon E-2300 Series (4 núcleos)
- **RAM:** 16 GB
- **Almacenamiento:** Mínimo 50 GB disponible
- **Conectividad:** Acceso a Internet + Intranet

### 2.2 Base de Datos SQL Server
- **Tipo:** SQL Server 2019 o posterior
- **Instalación:** Nueva instancia dedicada en el servidor
- **Autenticación:** Usuario/Contraseña (configurar en producción)
- **Ubicación:** Mismo servidor o servidor dedicado
- **Backup:** Diario (responsabilidad del cliente)

### 2.3 Navegadores Chromium
- **Cantidad Máxima Simultánea:** 3 (limitación PJUD)
- **Modo:** Headless (sin interfaz gráfica)
- **Ubicación:** Instalado con la aplicación
- **Puerto:** Dinámico (asignado por Playwright)

---

## 3. 📈 VOLUMEN DE DATOS Y CRECIMIENTO

| Fase | Período | Registros | Causas Esperadas | Almacenamiento |
|------|---------|-----------|------------------|-----------------|
| **Fase 1** | Mes 0-1 | 10,000 | ~30,000-50,000 | ~2 GB |
| **Fase 2** | Mes 1-3 | 25,000 | ~75,000-125,000 | ~5 GB |
| **Fase 3** | Mes 3-6 | 50,000 | ~150,000-250,000 | ~10 GB |

---

## 4. ⏰ HORARIOS Y ZONAS HORARIAS

### 4.1 Horarios de Operación
- **Inicio:** 08:00 AM
- **Fin:** 20:00 PM
- **Zona Horaria:** Chile (Única)
- **Comportamiento:** 
  - Fuera de horario: Aplicación pausada (no cierra)
  - Dentro de horario: Procesamiento continuo
  - Al reiniciar dentro del horario: Retoma desde donde quedó

### 4.2 Mantenimiento
- **Ventana:** 20:00 - 08:00 (12 horas)
- **Frecuencia:** Según necesidad (no programado)

---

## 5. 🔧 PLAN DE INSTALACIÓN Y CONFIGURACIÓN

### 5.1 Prerrequisitos en Servidor

#### Paso 1: Verificar Java
```bash
java -version
# Requerido: Java 21 o superior
# Ubicación: C:\Program Files\Java\jdk-21 (recomendado)
```

#### Paso 2: Instalar SQL Server 2019+
- Descargar desde: Microsoft Download Center
- Instalación: Instancia personalizada
- Configuración:
  - Autenticación: Mixta (SA + Windows)
  - Puerto: 1433 (default)
  - Collation: SQL_Latin1_General_CP1_CI_AS

#### Paso 3: Crear Base de Datos
```sql
-- Ejecutar con usuario SA
CREATE DATABASE veritusbot;
GO

USE veritusbot;
GO

-- Crear tablas base
CREATE TABLE personas (
    id UNIQUEIDENTIFIER DEFAULT NEWID() PRIMARY KEY,
    primer_nombre NVARCHAR(100) NULL,
    segundo_nombre NVARCHAR(100) NULL,
    apellido_paterno NVARCHAR(100) NULL,
    apellido_materno NVARCHAR(100) NULL,
    rut NVARCHAR(20) NULL
);

CREATE TABLE causas (
    id UNIQUEIDENTIFIER DEFAULT NEWID() PRIMARY KEY,
    persona_id UNIQUEIDENTIFIER NULL,
    rol NVARCHAR(50) NULL,
    anio INT NULL,
    caratula NVARCHAR(255) NULL,
    tribunal NVARCHAR(255) NULL,
    CONSTRAINT fk_persona_uuid FOREIGN KEY (persona_id) REFERENCES personas(id)
);

CREATE TABLE personas_procesadas (
    id INT IDENTITY(1,1) PRIMARY KEY,
    primer_nombre NVARCHAR(100) NULL,
    segundo_nombre NVARCHAR(100) NULL,
    apellido_paterno NVARCHAR(100) NULL,
    apellido_materno NVARCHAR(100) NULL,
    rut NVARCHAR(20) NULL,
    procesado BIT DEFAULT 0,
    creada DATETIME DEFAULT GETDATE(),
    fecha_procesada DATETIME NULL,
    tribunal_principal_procesado BIT DEFAULT 0
);

-- Crear índices para optimización
CREATE INDEX idx_personas_procesadas_procesado ON personas_procesadas(procesado);
CREATE INDEX idx_personas_procesadas_fecha ON personas_procesadas(fecha_procesada);
CREATE INDEX idx_causas_persona_id ON causas(persona_id);
CREATE INDEX idx_causas_anio ON causas(anio);

GO
```

#### Paso 4: Crear Usuario de Aplicación
```sql
-- No usar SA en producción
CREATE LOGIN appveritusbot WITH PASSWORD = 'PasswordSeguro123!@#';
GO

CREATE USER appveritusbot FOR LOGIN appveritusbot;
GO

-- Otorgar permisos específicos
GRANT SELECT, INSERT, UPDATE, DELETE ON personas TO appveritusbot;
GRANT SELECT, INSERT, UPDATE, DELETE ON causas TO appveritusbot;
GRANT SELECT, INSERT, UPDATE, DELETE ON personas_procesadas TO appveritusbot;

GO
```

### 5.2 Configuración de Aplicación

#### Paso 1: Preparar Carpetas
```bash
# En C:\veritusbot (o ruta elegida)
mkdir c:\veritusbot
mkdir c:\veritusbot\datos
mkdir c:\veritusbot\logs
mkdir c:\veritusbot\backup
```

#### Paso 2: Configurar application.properties
```properties
# === DATABASE ===
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=veritusbot
spring.datasource.username=appveritusbot
spring.datasource.password=PasswordSeguro123!@#
spring.datasource.driverClassName=com.microsoft.sqlserver.jdbc.SQLServerDriver

# === APPLICATION ===
server.port=8083
spring.application.name=veritusbot
logging.level.root=INFO
logging.level.com.example.veritusbot=DEBUG

# === ARCHIVOS ===
app.csv.personas.path=c:/veritusbot/datos/personas.csv
app.csv.resultados.path=c:/veritusbot/datos/resultados_busqueda.csv
app.logs.path=c:/veritusbot/logs/

# === HORARIOS ===
app.working.hours.start=08:00
app.working.hours.end=20:00
app.timezone=America/Santiago

# === WEB SCRAPING ===
app.max.threads=3
app.thread.delay.seconds=5
app.playwright.timeout.ms=30000
app.browser.headless=true

# === GRACEFUL SHUTDOWN ===
server.shutdown=graceful
spring.lifecycle.timeout-per-shutdown-phase=30s
```

#### Paso 3: Copiar Archivos Necesarios
```bash
# Estructura de carpetas
c:\veritusbot\
├── veritusbot-2.1-SNAPSHOT.jar
├── application.properties
├── datos\
│   ├── personas.csv          # Archivo de entrada
│   └── resultados_busqueda.csv # Archivo de salida (generado)
├── logs\
│   └── application.log       # Log principal
└── backup\
    └── resultados_backup_*.csv  # Copias de seguridad
```

#### Paso 4: Preparar personas.csv
```csv
NOMBRES,APELLIDO_PATERNO,APELLIDO_MATERNO,ANOINIT,ANOFIN
JUAN,PEREZ,GARCIA,2020,2026
MARIA,LOPEZ,MARTINEZ,2021,2026
CARLOS,RODRIGUEZ,FERNANDEZ,2022,2026
...
```

**Ubicación:** `C:\veritusbot\datos\personas.csv`

---

## 6. 🚀 PLAN DE DESPLIEGUE (DEPLOYMENT)

### 6.1 Checklist Pre-Despliegue

- [ ] Java 21+ instalado y configurado
- [ ] SQL Server 2019+ instalado
- [ ] Base de datos `veritusbot` creada
- [ ] Usuario `appveritusbot` creado con permisos
- [ ] Carpetas `c:\veritusbot\datos`, `logs`, `backup` creadas
- [ ] `application.properties` configurado con credenciales reales
- [ ] `personas.csv` en `c:\veritusbot\datos\`
- [ ] Conectividad INTERNET verificada
- [ ] Acceso a `https://oficinajudicialvirtual.pjud.cl` funcional
- [ ] Firewall configurado (puertos 8083, 1433 abiertos si es necesario)
- [ ] Antivirus excluya carpeta `c:\veritusbot` y procesos java/chrome

### 6.2 Proceso de Despliegue

#### Fase 1: Instalación Base (30 minutos)
1. Copiar `veritusbot-2.1-SNAPSHOT.jar` a `c:\veritusbot\`
2. Copiar `application.properties` a `c:\veritusbot\`
3. Copiar `personas.csv` a `c:\veritusbot\datos\`
4. Verificar permisos de lectura/escritura en carpetas

#### Fase 2: Prueba de Conectividad (15 minutos)
```bash
# Desde cmd en c:\veritusbot
# Probar conexión a BD
sqlcmd -S localhost -U appveritusbot -P "PasswordSeguro123!@#" -d veritusbot -Q "SELECT COUNT(*) FROM personas_procesadas"

# Resultado esperado: (0 rows affected) - significa que la tabla existe y es accesible
```

#### Fase 3: Prueba de Aplicación (30 minutos)
```bash
# Ejecutar en modo test (sin procesar)
cd c:\veritusbot
java -jar veritusbot-2.1-SNAPSHOT.jar --app.mode=test

# Verificar logs
# Esperado: Conexión a BD exitosa ✓
# Esperado: Validación de horario ✓
# Esperado: Lectura de personas.csv con N registros ✓
```

#### Fase 4: Marcha Blanca Inicial (2 horas)
```bash
# Ejecutar con muestra pequeña (10 personas)
# Crear personas_test.csv con 10 registros

java -jar veritusbot-2.1-SNAPSHOT.jar

# Monitorear en tiempo real:
# - Logs de consola
# - Resultados en resultados_busqueda.csv
# - Datos en BD (tabla causas y personas_procesadas)
```

#### Fase 5: Despliegue Completo (1 día)
```bash
# Ejecutar con todos los registros (10,000 en Fase 1)
java -jar veritusbot-2.1-SNAPSHOT.jar

# Dejar procesando durante horario laboral (08:00-20:00)
# Verificar progreso cada 2 horas
```

### 6.3 Servicio Windows (Recomendado para producción)

Para ejecutar como servicio automático:

1. **Descargar NSSM** (Non-Sucking Service Manager)
   ```bash
   # Descargar de: https://nssm.cc/download
   # Extraer a: C:\nssm\
   ```

2. **Registrar Servicio**
   ```bash
   C:\nssm\nssm install veritusbot "C:\Program Files\Java\jdk-21\bin\java.exe" "-jar C:\veritusbot\veritusbot-2.1-SNAPSHOT.jar"
   
   C:\nssm\nssm set veritusbot AppDirectory "C:\veritusbot"
   C:\nssm\nssm set veritusbot AppStdout "C:\veritusbot\logs\service.log"
   C:\nssm\nssm set veritusbot AppStderr "C:\veritusbot\logs\service-error.log"
   ```

3. **Iniciar Servicio**
   ```bash
   net start veritusbot
   ```

4. **Verificar Estado**
   ```bash
   # En Services.msc buscar: veritusbot
   # Debe estar "Running"
   ```

---

## 7. ✅ PLAN DE VALIDACIÓN Y PRUEBAS

### 7.1 Pruebas de Conectividad

| Test | Procedimiento | Resultado Esperado |
|------|---------------|--------------------|
| **BD Disponible** | `SELECT 1` en conexión | Conexión exitosa |
| **Tablas Existen** | Contar registros en tablas | Tablas vacías o con datos |
| **Usuarios Permisos** | Insertar un registro test | INSERT exitoso |
| **PJUD Accesible** | Abrir en navegador | Sitio disponible (no bloqueado) |
| **Internet OK** | ping 8.8.8.8 | Respuesta exitosa |

### 7.2 Pruebas Funcionales

#### Test 1: Lectura de CSV
```
Entrada: personas.csv con 5 registros
Proceso: Leer y sincronizar con BD
Resultado: Verificar 5 registros en personas_procesadas con procesado=0
```

#### Test 2: Búsqueda Simple
```
Entrada: 1 persona (JUAN PEREZ GARCIA, años 2020-2026)
Proceso: Buscar en tribunal específico (1º Juzgado Civil Santiago)
Resultado: Si hay causas → Se guardan en CSV y BD | Si no → Se registra intento
```

#### Test 3: Búsqueda Completa
```
Entrada: 10 personas (muestra de personas.csv)
Proceso: Ejecutar búsqueda completa (Fase 4.1 y 4.2)
Resultado: 
- Personas marcadas como procesado=1
- Causas guardadas en CSV y BD
- tribunal_principal_procesado=1 después de Fase 4.1
```

#### Test 4: Horarios
```
Caso 1: Ejecutar a las 19:00
Resultado: Sigue procesando hasta 20:00, luego se pausa

Caso 2: Ejecutar a las 21:00
Resultado: Se pausa hasta 08:00 del día siguiente

Caso 3: Dejar ejecutándose múltiples días
Resultado: Continúa automáticamente sin intervención
```

#### Test 5: Recuperación de Fallos
```
Caso 1: Cerrar Chrome durante búsqueda
Resultado: Reinicia automáticamente, continúa donde quedó

Caso 2: Desconexión BD momentánea
Resultado: Reintentos automáticos, continúa sin pérdida de datos

Caso 3: Parada de aplicación (Ctrl+C)
Resultado: Graceful shutdown, no corrupción de BD
```

### 7.3 Pruebas de Rendimiento

#### Test de Carga Progresiva
| Fase | Personas | Duración Esperada | CPU | RAM |
|------|----------|------------------|-----|-----|
| 1 | 10 | 30 min | <30% | <500MB |
| 2 | 100 | 5 horas | <50% | <800MB |
| 3 | 1,000 | 50 horas | <60% | <1GB |
| 4 | 10,000 | ~20 días | <70% | <1.2GB |

---

## 8. 📊 MONITOREO EN PRODUCCIÓN

### 8.1 Verificaciones Diarias (Manuales)

**Checklist (08:00 AM):**
- [ ] Servicio veritusbot está corriendo
- [ ] Archivo `application.log` tiene entradas recientes
- [ ] No hay errores en logs de últimas 24 horas
- [ ] `resultados_busqueda.csv` tiene datos actualizados
- [ ] Conexión a BD funciona

**Comando de verificación:**
```bash
# Verificar servicio corriendo
tasklist | findstr java

# Ver últimas líneas del log
type c:\veritusbot\logs\application.log | findstr /E "ERROR FATAL" || echo "Sin errores"

# Contar registros procesados
sqlcmd -S localhost -U appveritusbot -P "..." -d veritusbot -Q "SELECT COUNT(*) FROM personas_procesadas WHERE procesado=1"
```

### 8.2 Métricas a Rastrear

- **Personas Procesadas:** Cantidad total vs. meta
- **Causas Encontradas:** Total de causas en BD
- **Tasa de Error:** Fallos durante búsqueda
- **Disponibilidad:** Tiempo de uptime (objetivo 99.9%)
- **Espacio en Disco:** Crecimiento de datos

### 8.3 Alertas Críticas

Aunque NO se solicita monitoreo automatizado, registrar:

- ❌ Aplicación se detiene sin razón (revisar logs)
- ❌ Conexión a BD falla (revisar credenciales)
- ❌ PJUD no responde (esperar y reintentar)
- ❌ Espacio en disco < 1 GB (limpiar backup viejo)
- ❌ CSV corrompido (restaurar del backup)

---

## 9. 🔒 PLAN DE SEGURIDAD

### 9.1 Credenciales y Contraseñas

**NUNCA en application.properties:**
```properties
# ❌ EVITAR
spring.datasource.password=micontraseña123
```

**Alternativa: Variables de Entorno**
```bash
# En Windows cmd o System Properties
setx DB_PASSWORD "PasswordSeguro123!@#"
setx DB_USER "appveritusbot"
```

**En application.properties:**
```properties
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}
```

### 9.2 Permisos de Archivos

```bash
# Carpeta de datos: Solo administrador y aplicación
# icacls C:\veritusbot /grant:r "USERS":(OI)(CI)(M)
```

### 9.3 Comunicaciones

- ✅ PJUD: Conexión HTTPS (certificado validado)
- ✅ BD: Autenticación usuario/contraseña
- ✅ Logs: No guardar passwords (implementado en código)

---

## 10. 📅 CRONOGRAMA DE EJECUCIÓN

### Semana 1: Preparación Infraestructura

| Día | Tarea | Responsable |
|-----|-------|-------------|
| **Lunes** | Instalar Java 21, SQL Server | IT Escritorio |
| **Martes** | Crear BD, usuario, tablas | DBA / IT |
| **Miércoles** | Preparar carpetas, permisos | IT |
| **Jueves** | Configurar application.properties | Dev + IT |
| **Viernes** | Validaciones de conectividad | QA |

### Semana 2: Despliegue Marcha Blanca

| Día | Tarea | Duración |
|-----|-------|----------|
| **Lunes** | Instalación de JAR y configuración | 1 hora |
| **Martes** | Test de 10 personas (Fase 1) | 2 horas |
| **Miércoles** | Test de 100 personas (Fase 2) | 4 horas |
| **Jueves** | Test de 1,000 personas | 8 horas |
| **Viernes** | Validaciones finales, Go Live con 10K | 1 hora |

### Semana 3+: Operación Productiva

- **Semana 3-4:** Monitoreo diario, optimización
- **Mes 1:** Escalado a 25,000 registros
- **Mes 2-3:** Escalado a 50,000 registros
- **Mes 6:** Evaluación y plan futuro

---

## 11. 🚨 PLAN DE CONTINGENCIA Y ROLLBACK

### 11.1 Escenarios de Fallo

#### Escenario 1: Aplicación no inicia
```
Síntoma: java.exe no responde o error al iniciar
Causa probable: Java no instalado, PORT 8083 ocupado, BD no disponible
Solución:
1. Verificar: java -version
2. Cambiar puerto en application.properties
3. Verificar BD con: sqlcmd
4. Revisar logs en c:\veritusbot\logs\
```

#### Escenario 2: Conexión BD falla
```
Síntoma: "Connection refused" en logs
Causa probable: Credenciales incorrectas, SQL Server no iniciado
Solución:
1. Verificar SQL Server está ejecutándose: services.msc
2. Validar credenciales: sqlcmd -S localhost -U appveritusbot
3. Reiniciar SQL Server
```

#### Escenario 3: PJUD bloquea IP
```
Síntoma: Timeouts constantes en búsquedas
Causa probable: Demasiadas conexiones desde misma IP
Solución (implementada):
- Ya limitado a 3 threads máximo
- Esperar 24 horas para que se levante bloqueo
- No necesario aumentar threads
```

#### Escenario 4: Corrupción de BD
```
Síntoma: Errores SQL, datos inconsistentes
Causa probable: Caída de servidor, write I/O error
Solución:
1. DETENER aplicación (net stop veritusbot)
2. Ejecutar DBCC CHECKDB en SQL Server
3. Restaurar backup del día anterior
4. Reiniciar aplicación
```

### 11.2 Procedimiento de Rollback

**NO IMPLEMENTADO en esta fase** (según requerimiento)

Si fuera necesario en futuro:
1. Mantener versión anterior del JAR
2. Mantener backup de BD
3. Cambiar `application.properties` a versión anterior
4. Reiniciar servicio

---

## 12. 📞 SOPORTE Y ESCALAMIENTO

### 12.1 Canales de Soporte

| Problema | Contacto | Tiempo Respuesta |
|----------|----------|------------------|
| Errores Técnicos | Desarrollador | <4 horas |
| Problemas Servidor | IT Escritorio | <2 horas |
| Datos/BD | DBA | <4 horas |

### 12.2 Logs para Análisis

En caso de problema, recopilar:
1. `c:\veritusbot\logs\application.log` (últimas 100 líneas)
2. Error en `resultados_busqueda.csv`
3. Output de: `SELECT * FROM personas_procesadas WHERE procesado=0 LIMIT 5`
4. Estado del servicio: `Get-Service veritusbot | Select *` (PowerShell)

---

## 13. ✨ CRITERIOS DE ÉXITO

### Marcha Blanca EXITOSA si:

✅ **Día 1 (Instalación)**
- [ ] Aplicación inicia sin errores
- [ ] Conexión a BD verificada
- [ ] Archivos de configuración correctos

✅ **Día 2-3 (Pruebas Funcionales)**
- [ ] Busca 10 personas exitosamente
- [ ] Resultados guardados en CSV y BD
- [ ] Marca de "procesado" funciona

✅ **Día 4-5 (Pruebas Volumen)**
- [ ] Procesa 100-1,000 personas sin crashes
- [ ] Recuperación de fallos funciona
- [ ] Horarios respetados

✅ **Día 6+ (Producción)**
- [ ] Procesando 10,000 personas iniciales
- [ ] Uptime >= 99% en horario laboral
- [ ] Sin corrupción de datos
- [ ] Crecimiento ordenado a 50K en 6 meses

---

## 14. 📝 DOCUMENTACIÓN ENTREGADA

- ✅ Este Plan de Marcha Blanca
- ✅ README.md (arquitectura general)
- ✅ DIAGRAMA_FLUJO_COMPLETO_DRAWIO.xml
- ✅ DIAGRAMA_FLUJO_COMPLETO.xml
- ✅ application.properties (template)
- ✅ Script SQL de tablas
- ✅ Script de creación de usuario

---

## 15. 🎯 PRÓXIMOS PASOS

1. **Confirmación de Fecha:** Agendar marcha blanca
2. **Preparación Servidor:** IT instala Java y SQL Server
3. **Entrega de Credenciales:** Cliente proporciona usuario/password SQL Server
4. **Ejecución Plan:** Seguir cronograma semana 1-2
5. **Go Live:** Cuando se cumplen criterios de éxito
6. **Soporte Post-Lanzamiento:** Monitoreo durante primer mes

---

## 📌 APROBACIONES REQUERIDAS

| Rol | Nombre | Fecha | Firma |
|-----|--------|-------|-------|
| Desarrollo | [Developer] | ___/___/___ | _____ |
| Cliente | [Representante] | ___/___/___ | _____ |
| IT Servidor | [IT Manager] | ___/___/___ | _____ |

---

**Documento Versión:** 1.0  
**Próxima Revisión:** Post-Lanzamiento (Semana 3)  
**Última Actualización:** 8 de Marzo de 2026

---

**🚀 ¡Listo para Marcha Blanca!**

