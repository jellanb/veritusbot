# Veritus Bot - Documentación Técnica

## 🎯 Descripción General

Veritus Bot es una aplicación Spring Boot que automatiza búsquedas de información en el sistema de Oficina Judicial Virtual (PJUD) de Chile. Utiliza Playwright para navegación automática y Chromium para scraping de datos.

## ✨ Características Principales

### 1. Validación Automática de Base de Datos
- La aplicación valida la conexión a SQL Server al iniciar
- Si la BD está disponible → Continúa normalmente ✅
- Si la BD NO está disponible → Muestra error y apaga gracefully ❌
- Implementación: `DatabaseStartupValidator.java`

### 2. Arquitectura Modular y Escalable (NEW)
- **ScraperOrchestrator:** Punto de entrada que coordina toda la búsqueda
- **BrowserManager:** Gestión del ciclo de vida de Playwright
- **FormFiller:** Llena formularios y campos
- **TribunalSelector:** Maneja dropdown de tribunales
- **ResultParser:** Parsea resultados HTML
- **Phase1Scraper:** Búsqueda en tribunales de Santiago (1-30)
- **Phase2Scraper:** Búsqueda en otros tribunales
- Implementación: `service/scraper/` con arquitectura SOLID y Dependency Injection

### 3. Búsqueda en Dos Fases
- **Fase 1:** Procesa TODOS los tribunales de Santiago (1º-30º Juzgado Civil)
- **Fase 2:** Procesa el resto de tribunales (excluyendo Santiago)
- Después de Fase 1, marca `tribunal_principal_procesado = true`
- Permite priorizar tribunales más consultados
- Implementación: `Phase1Scraper` y `Phase2Scraper` ejecutadas por `ScraperOrchestrator`

### 4. Recuperación Automática de Progreso
- Guarda el estado después de procesar cada tribunal
- Recupera automáticamente si ocurre un error (Chromium se cierra)
- Continúa desde donde se quedó (no repite tribunales)
- Limpia el progreso al completar exitosamente
- Implementación: `SearchProgressManager.java`

### 5. Rango Horario de Operación
- Trabaja automáticamente solo entre 8 AM y 8 PM
- Se pausa automáticamente fuera de este rango
- Continúa automáticamente al día siguiente
- Termina únicamente cuando procesa todos los clientes
- Soporta búsquedas multi-día
- Implementación: `WorkingHoursManager.java`

### 6. Registro de Personas Procesadas
- Guarda todas las personas del Excel en tabla `personas_procesadas`
- Registra fecha y hora de creación
- Marca como procesada cuando se completa la búsqueda
- Registra fecha y hora de procesamiento
- Permite auditoría y rastreo de qué personas se han procesado
- Implementación: `PersonaProcesada.java` y `PersonaProcesadaRepository.java`

### 7. ✨ Sistema de Autenticación JWT (NEW) ✨
- **Autenticación basada en JWT (JSON Web Tokens)**
- Endpoint `/api/veritus-app/login` para autenticar usuarios
- **Roles:** ADMIN, OPERADOR, VIEWER
- Protección de endpoints sensibles con Spring Security
- Validación de tokens en cada request
- Manejo de CORS para frontend
- BCrypt para hash de contraseñas
- Expiración de tokens en 24 horas
- Auditoría: registro de último login
- Implementación: `service/auth/`, `controller/LoginController.java`, `config/SecurityConfig.java`

**Documentación:** Ver [`AUTENTICACION.md`](./AUTENTICACION.md) y [`GUIA_RAPIDA_LOGIN.md`](./GUIA_RAPIDA_LOGIN.md)

## 🚀 Inicio Rápido

### Paso 1: Iniciar SQL Server
```bash
podman run -d \
  --name sqlserver \
  -e "ACCEPT_EULA=Y" \
  -e "MSSQL_SA_PASSWORD=SqlServer2026Strong" \
  -e "MSSQL_MEMORY_LIMIT_MB=512" \
  -p 14333:1433 \
  mcr.microsoft.com/mssql/server:2022-latest

sleep 20
```

### Paso 2: Compilar y Ejecutar
```bash
cd /Users/jellan/Documents/git/veritusbot
mvn clean package -DskipTests
java -jar target/veritusbot-0.0.1-SNAPSHOT.jar
```

### Paso 3: Usar la Aplicación
```bash
# En otra terminal - Test simple
curl "http://localhost:8083/api/test"
# Resultado: ✓ API funcionando correctamente

# 🔐 NUEVO: Autenticarse
curl -X POST http://localhost:8083/api/veritus-app/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@veritus.com","password":"admin123"}'
# Resultado: JWT token + datos usuario

# Búsqueda completa (requiere token)
TOKEN="<jwt-token-aqui>"
curl -X POST "http://localhost:8083/api/buscar-personas?archivo=personas.csv" \
  -H "Authorization: Bearer $TOKEN"
```

## 📊 Configuración

### application.properties
```properties
server.port=8083
spring.datasource.url=jdbc:sqlserver://localhost:14333;databaseName=veritus;encrypt=true;trustServerCertificate=true
spring.datasource.username=sa
spring.datasource.password=SqlServer2026Strong
spring.datasource.driver-class-name=com.microsoft.sqlserver.jdbc.SQLServerDriver
```

### Base de Datos
- **Motor:** SQL Server 2022
- **Puerto:** 14333
- **Usuario:** sa
- **Contraseña:** SqlServer2026Strong
- **BD:** veritus

**Tablas:**
```sql
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
    procesado BIT NULL,
    creada DATETIME NULL,
    fecha_procesada DATETIME NULL
);
```

## 📋 Archivos Clave

### Nuevos
- `src/main/java/com/example/veritusbot/config/DatabaseStartupValidator.java` - Validador de BD

### Modificados
- `src/main/java/com/example/veritusbot/scraper/PjudScraper.java` - Búsqueda con delays

### Configuración
- `src/main/resources/application.properties` - Propiedades de Spring
- `pom.xml` - Dependencias Maven

## 📝 Sistema de Logging Avanzado

### Configuración de Entorno

En `application.properties`, configura el entorno:

```properties
app.environment=desarrollo
# Valores: desarrollo, produccion, pruebas
```

### Niveles de Log

- **🔍 DEBUG** - Información detallada (solo en desarrollo)
- **ℹ️ INFO** - Información general (desarrollo, pruebas)
- **⚠️ WARNING** - Advertencias (todos los entornos)
- **❌ ERROR** - Errores críticos (todos los entornos)

### Comportamiento por Entorno

| Entorno | Niveles | Uso |
|---------|---------|-----|
| **desarrollo** | DEBUG, INFO, WARNING, ERROR | Desarrollo local con todos los detalles |
| **pruebas** | INFO, WARNING, ERROR | Testing sin información de debug |
| **produccion** | WARNING, ERROR | Solo logs críticos y esenciales |

### Uso en el Código

```java
// DEBUG - Solo en desarrollo
logger.debug("Variable: " + value);
logger.debug("Año %d, delay %d segundos", year, delay);

// INFO - En desarrollo y pruebas
logger.info("Procesando solicitud");
logger.info("Total registros: %d", count);

// WARNING - Todos los entornos
logger.warning("Posible problema: %s", issue);

// ERROR - Todos los entornos
logger.error("Error crítico: %s", error);

// Secciones visuales
logger.section("INICIANDO BÚSQUEDA");
```

### Clase LoggerUtil

**Ubicación:** `src/main/java/com/example/veritusbot/util/LoggerUtil.java`

**Métodos principales:**
- `debug(String, Object...)` - Log de debug
- `info(String, Object...)` - Log de información
- `warning(String, Object...)` - Log de advertencia
- `error(String, Object...)` - Log de error
- `section(String)` - Sección con separadores visuales
- `getCurrentEnvironment()` - Obtiene el entorno actual

## 🔄 Flujo de Funcionamiento

### 1. Startup
```
Spring inicia
  ↓
DatabaseStartupValidator valida conexión a BD
  ├─ ✅ BD disponible → Continúa
  └─ ❌ BD no disponible → Apaga gracefully
```

### 2. Búsqueda (buscarPersonasDelExcel)
```
Lee archivo CSV/Excel con lista de personas
  ↓
Para cada persona:
  ├─ Calcula rango de años (ANIO_INIT a ANIO_FIN)
  ├─ Crea ScheduledExecutorService (máx 3 threads)
  ├─ Para cada año, programa búsqueda con delay escalonado:
  │  ├─ Año 1: t=0s
  │  ├─ Año 2: t=3s
  │  ├─ Año 3: t=6s
  │  └─ Año N: t=(N-1)*3s
  └─ Espera a que terminen todas las búsquedas
  ↓
Guarda resultados en CSV y BD
```

### 3. Búsqueda Individual (buscarPorNombreParalelo)
```
Abre navegador Chromium
  ↓
Navega a: https://oficinajudicialvirtual.pjud.cl/indexN.php
  ↓
Completa formulario:
  ├─ Cierra popup modal
  ├─ Selecciona "Búsqueda por Nombre"
  ├─ Competencia: Civil (valor 3)
  ├─ Nombres, Apellidos, Año
  └─ Abre dropdown de tribunales
  ↓
Itera cada tribunal (máx 231):
  ├─ Selecciona tribunal por índice
  ├─ Hace clic en "Buscar"
  ├─ Extrae resultados (si existen)
  └─ Continúa siguiente tribunal
  ↓
Cierra navegador
  ↓
Agrega resultados a lista compartida (thread-safe)
```

## 🔌 API Endpoints

### GET /api/test
- **Descripción:** Test de la API
- **Respuesta:** `✓ API funcionando correctamente`

### GET /api/buscar-personas
- **Descripción:** Inicia búsqueda desde archivo CSV
- **Parámetros:** 
  - `archivo` (default: personas.csv)
- **Respuesta:** `✓ Búsqueda completada. Revisa los logs.`

## 📁 Estructura del Proyecto

```
veritusbot/
├── src/main/java/com/example/veritusbot/
│   ├── VeritusbotApplication.java         (Clase principal)
│   ├── config/
│   │   ├── WebDriverConfig.java           (Configuración Playwright)
│   │   ├── DatabaseStartupValidator.java  (Validación de BD)
│   │   ├── GracefulShutdownConfig.java    (Apagado elegante)
│   │   └── ResourceCleanupManager.java    (Limpieza de recursos)
│   ├── controller/
│   │   └── BusquedaController.java        (Endpoints REST)
│   ├── model/
│   │   ├── Causa.java
│   │   ├── Persona.java
│   │   └── PersonaProcesada.java
│   ├── repository/
│   │   ├── CausaRepository.java
│   │   ├── PersonaRepository.java
│   │   └── PersonaProcesadaRepository.java
│   ├── scraper/
│   │   ├── PjudScraper.java              (Lógica principal de scraping)
│   │   ├── SearchProgressManager.java    (Recuperación de progreso)
│   │   └── WorkingHoursManager.java      (Rango horario de operación)
│   ├── service/
│   │   ├── PersonaService.java
│   │   ├── CausaService.java
│   │   └── LoggingService.java           (Sistema de logs)
│   ├── util/
│   │   ├── ExcelReader.java              (Lectura de Excel)
│   │   ├── CustomLogger.java             (Logger personalizado)
│   │   └── LogLevel.java                 (Niveles de log)
│   └── dto/
│       ├── PersonaDTO.java
│       ├── CausaDTO.java
│       └── RolDTO.java
├── src/main/resources/
│   ├── application.properties             (Configuración)
│   └── logback.xml                       (Configuración de logs)
├── pom.xml                               (Dependencias Maven)
├── personas.csv                          (Datos de entrada)
├── resultados_busqueda.csv               (Resultados de búsqueda)
└── README.md                             (Este archivo)
```

## 📁 Estructura de Datos

### Entrada (personas.csv)
```csv
NOMBRES,APELLIDO PATERNO,APELLIDO MATERNO,ANIO_INIT,ANIO_FIN
MIGUEL ANTONIO,SOTO,FREDES,2019,2024
JUAN CARLOS,GARCÍA,MARTÍNEZ,2020,2023
```

### Salida (resultados_busqueda.csv)
```csv
Nombres,Apellido Paterno,Apellido Materno,Año,Rol,Fecha,Caratulado,Tribunal
MIGUEL ANTONIO,SOTO,FREDES,2024,C-3662-2024,27/02/2024,SANTANDER CONSUMER FINANCE LTDA./SOTO,6º Juzgado Civil de Santiago
```

## 🛑 Shutdown Graceful

### Ctrl+C durante ejecución
```
Inicia cierre elegante
  ↓
GracefulShutdownConfig ejecuta limpieza
  ├─ Cierra navegadores Chromium
  ├─ Detiene ScheduledExecutorService (espera máx 60s)
  ├─ Cierra conexiones a BD
  └─ Libera recursos
  ↓
Aplicación se apaga correctamente
```

## 🔧 Dependencias Principales

- **Spring Boot:** 3.2.5
- **Java:** 21+
- **Microsoft Playwright:** 1.42.0+
- **JSoup:** 1.17.2+
- **Microsoft SQL Server JDBC:** Incluido
- **Maven:** 3.8+

## 🐛 Troubleshooting

### Error: "Could not find or load main class"
```bash
mvn clean compile
mvn package -DskipTests
```

### Error: "Schema-validation: missing table"
Verifica que `spring.jpa.hibernate.ddl-auto=update` en `application.properties`

### "Cannot connect to BD"
```bash
# Verificar que SQL Server está corriendo
podman ps | grep sqlserver

# Si no está, iniciar
podman start sqlserver
sleep 20
java -jar target/veritusbot-0.0.1-SNAPSHOT.jar
```

### "Address already in use: 14333"
```bash
# Eliminar y recrear contenedor
podman rm -f sqlserver
podman run -d --name sqlserver \
  -e "ACCEPT_EULA=Y" \
  -e "MSSQL_SA_PASSWORD=SqlServer2026Strong" \
  -e "MSSQL_MEMORY_LIMIT_MB=512" \
  -p 14333:1433 \
  mcr.microsoft.com/mssql/server:2022-latest
```

## 🧪 Pruebas Rápidas

### Test 1: Validación de BD
```bash
podman stop sqlserver
java -jar target/veritusbot-0.0.1-SNAPSHOT.jar
# Debe fallar indicando error de conexión
```

### Test 2: Verificar Delays
```bash
curl "http://localhost:8083/api/buscar-personas?archivo=personas.csv"
# Observar logs con delays escalonados
```

### Test 3: Máximo de Navegadores
```bash
pgrep -c chromium
# Resultado esperado: máximo 3
```

## 📊 Monitoreo

### Logs en tiempo real
```bash
tail -f target/application.log
```

### Ver navegadores activos
```bash
watch -n 1 'pgrep -c chromium'
```

### Estado de la BD
```sql
SELECT COUNT(*) FROM personas_procesadas WHERE procesado = 1;
SELECT COUNT(*) FROM causas;
```

## 🚀 Deploy en VPS - Guía Paso a Paso

### Requisitos en el VPS
- Java 21+
- SQL Server 2022+ (o conexión a BD existente)
- Git instalado
- 4GB RAM mínimo
- 2 cores CPU
- Acceso a internet
- Acceso a los proxies configurados

### 🔄 Descargar e Instalar Cambios en VPS

#### **Paso 1: Conectar al VPS**
```bash
ssh usuario@tu-vps.com
# O si tienes alias
ssh tu-vps
```

#### **Paso 2: Navegar al Directorio del Proyecto**
```bash
cd ~/veritusbot
# O el directorio donde tengas el proyecto
# Por ejemplo: /opt/veritusbot o /home/usuario/veritusbot
```

#### **Paso 3: Descargar los Cambios desde Git**
```bash
# Si es la primera vez (clonar repositorio)
cd ~
git clone https://github.com/TU_USUARIO/veritusbot.git
cd veritusbot

# Si ya tienes el repositorio (traer cambios)
git pull origin main
# O si usas rama diferente
git pull origin tu-rama
```

#### **Paso 4: Revisar los Cambios (Opcional)**
```bash
# Ver qué cambió
git log --oneline -5

# Ver archivos modificados
git status

# Ver cambios específicos en application.properties
git diff src/main/resources/application.properties
```

#### **Paso 5: Compilar la Aplicación**
```bash
# Limpiar compilación anterior
./mvnw clean

# Compilar sin ejecutar tests (más rápido)
./mvnw package -DskipTests

# Si quieres ejecutar tests también (más lento)
./mvnw clean package
```

#### **Paso 6: Detener la Versión Anterior (si está corriendo)**
```bash
# Si está corriendo como proceso foreground
# Presiona Ctrl+C

# Si está corriendo como background/servicio
pkill -f "java -jar"
# O si usas systemd
sudo systemctl stop veritusbot
```

#### **Paso 7: Ejecutar la Nueva Versión**

**Opción A: Ejecución Manual (para pruebas)**
```bash
java -jar target/veritusbot-0.0.1-SNAPSHOT.jar
```

**Opción B: Ejecución en Background**
```bash
nohup java -jar target/veritusbot-0.0.1-SNAPSHOT.jar > app.log 2>&1 &

# Ver que está corriendo
ps aux | grep java

# Ver logs en tiempo real
tail -f app.log
```

**Opción C: Ejecución con More Memory**
```bash
# Si tienes suficiente memoria
nohup java -Xmx2G -jar target/veritusbot-0.0.1-SNAPSHOT.jar > app.log 2>&1 &
```

**Opción D: Crear Servicio Systemd (Recomendado para producción)**
```bash
# Crear archivo de servicio
sudo nano /etc/systemd/system/veritusbot.service
```

Pega el siguiente contenido:
```ini
[Unit]
Description=Veritus Bot - PJUD Scraper
After=network.target

[Service]
Type=simple
User=tu-usuario
WorkingDirectory=/home/tu-usuario/veritusbot
ExecStart=/usr/bin/java -Xmx2G -jar target/veritusbot-0.0.1-SNAPSHOT.jar
Restart=on-failure
RestartSec=10s
StandardOutput=append:/home/tu-usuario/veritusbot/app.log
StandardError=append:/home/tu-usuario/veritusbot/app.log

[Install]
WantedBy=multi-user.target
```

Luego:
```bash
# Recargar systemd
sudo systemctl daemon-reload

# Iniciar el servicio
sudo systemctl start veritusbot

# Habilitarlo para que inicie con el VPS
sudo systemctl enable veritusbot

# Ver estado
sudo systemctl status veritusbot

# Ver logs
journalctl -u veritusbot -f
```

### ✅ Navegador en Modo Headless

**Desde v2.3:** Por defecto, Chromium se ejecuta en modo `headless=true` (sin interfaz gráfica).

Esto es ideal para VPS sin servidor X11. El navegador funciona correctamente sin GUI.

#### Para Desarrollo Local (Ver navegador con interfaz)

Si quieres ver el navegador funcionando en tu máquina local:

```bash
# Edita application.properties
app.scraper.browser.headless=false

# Ejecuta
java -jar target/veritusbot-0.0.1-SNAPSHOT.jar
```

El navegador aparecerá en pantalla mientras ejecuta las búsquedas.

### ✅ Verificar que la Aplicación Está Corriendo

```bash
# Verificar que el puerto 8083 esté abierto
netstat -tuln | grep 8083
# O
ss -tuln | grep 8083

# Hacer un test a la API
curl http://localhost:8083/api/test
# Esperado: ✓ API funcionando correctamente

# Verificar logs
tail -f app.log
```

### 🔍 Verificar Configuración de Proxies

En la aplicación:
```bash
# Ver que los proxies están cargados
grep -E "proxi|userProxi|passProxi" src/main/resources/application.properties
```

En logs durante ejecución (buscar):
```
Using random proxy for this browser instance
```

### 🐛 Troubleshooting - Errores Comunes

#### Error: "Cannot connect to SQL Server"
```bash
# Verificar que SQL Server esté disponible
# Si está local en el VPS
docker ps | grep sqlserver
# O
systemctl status mssql-server

# Si es BD remota, verificar conectividad
telnet bd-servidor.com 1433
```

#### Error: "Address already in use: 8083"
```bash
# Matar proceso en puerto 8083
sudo lsof -i :8083
sudo kill -9 <PID>

# O cambiar puerto en application.properties
# server.port=8084
```

#### Error: "Permission denied" o "No such file or directory"
```bash
# Hacer scripts ejecutables
chmod +x mvnw

# O usar Maven instalado globalmente
mvn clean package -DskipTests
```

#### Error: "OutOfMemoryError"
```bash
# Aumentar memoria
java -Xmx3G -jar target/veritusbot-0.0.1-SNAPSHOT.jar
```

### 📝 Script Automatizado (Opcional)

Crea un archivo `deploy-vps.sh` en el raíz del proyecto:

```bash
#!/bin/bash
set -e

echo "🚀 Iniciando deploy en VPS..."

# Paso 1: Descargar cambios
echo "📥 Descargando cambios..."
git pull origin main

# Paso 2: Detener versión anterior
echo "⏹️  Deteniendo versión anterior..."
pkill -f "java -jar" || true
sleep 3

# Paso 3: Compilar
echo "🔨 Compilando..."
./mvnw clean package -DskipTests

# Paso 4: Iniciar nueva versión
echo "🚀 Iniciando nueva versión..."
nohup java -Xmx2G -jar target/veritusbot-0.0.1-SNAPSHOT.jar > app.log 2>&1 &

sleep 5

# Paso 5: Verificar
echo "✅ Verificando que está corriendo..."
if pgrep -f "java -jar" > /dev/null; then
    echo "✅ Aplicación iniciada correctamente"
    echo "📊 Ver logs: tail -f app.log"
    curl http://localhost:8083/api/test || echo "⏳ Espera a que la aplicación termine de iniciar..."
else
    echo "❌ Error: Aplicación no inició"
    tail -20 app.log
fi
```

Usa:
```bash
chmod +x deploy-vps.sh
./deploy-vps.sh
```

---

## 🚀 Deploy en Producción (Configuración Avanzada)

### Requisitos
- Java 21+
- SQL Server 2022+
- 4GB RAM mínimo
- 2 cores CPU
- Acceso a internet

### Pasos Principales
1. Compilar: `./mvnw clean package`
2. Copiar JAR a servidor
3. Configurar `application.properties` con credenciales reales
4. Asegurar que SQL Server está disponible
5. Ejecutar con opciones de memoria: `java -Xmx2G -jar veritusbot-0.0.1-SNAPSHOT.jar`

---

**Versión:** 2.3.1 (Fix: Headless Browser Mode por Defecto)  
**Última actualización:** 30 Marzo 2026  
**Estado:** PRODUCTIVO ✅

### 📋 Cambios Recientes (v2.3.1)
- ✅ **Fix Critical:** Chromium ahora corre en modo `headless=true` por defecto (sin X Server necesario)
- ✅ **VPS Compatible:** Ya no requiere `xvfb-run` para ejecutar en VPS
- ✅ **Local Configurable:** Opción `app.scraper.browser.headless=false` para debugging con GUI
- ✅ **Error Resuelto:** "Missing X server or $DISPLAY" ya no ocurre en VPS

### 📋 Cambios Anteriores (v2.3)
- ✅ **Soporte de Proxies Aleatorios:** Configura 6 proxies con usuario/contraseña en `application.properties`
- ✅ **Selección Aleatoria de Proxy:** Cada navegador elige un proxy aleatoriamente por cliente/año
- ✅ **Aumento de Tiempos:** +3 segundos antes de cada interacción (click, select, fill, submit)
- ✅ **Logs Mejorados:** Muestra qué proxy se está usando en cada instancia del navegador
- ✅ **Guía de Deploy VPS:** Pasos completos para descargar e instalar cambios en VPS

