# Veritus Bot - Documentación Técnica

## 🎯 Descripción General

Veritus Bot es una aplicación Spring Boot que automatiza búsquedas de información en el sistema de Oficina Judicial Virtual (PJUD) de Chile. Utiliza Playwright para navegación automática y Chromium headless para scraping de datos.

## ✨ Características Principales

### 1. Validación Automática de Base de Datos
- La aplicación valida la conexión a SQL Server al iniciar
- Si la BD está disponible → Continúa normalmente ✅
- Si la BD NO está disponible → Muestra error y apaga gracefully ❌
- Implementación: `DatabaseStartupValidator.java`

### 2. Búsqueda Paralela con Delays Controlados
- Busca información de personas en múltiples años
- Abre navegadores de forma escalonada (3 segundos entre cada uno)
- Máximo 3 navegadores simultáneos
- Evita sobrecargar el servidor de PJUD
- Implementación: `PjudScraper.java` con `ScheduledExecutorService`

### 3. Búsqueda en Dos Fases por Tribunal
- **Fase 1:** Procesa primero TODOS los tribunales de Santiago (1º-30º Juzgado Civil de Santiago)
- **Fase 2:** Procesa el resto de los tribunales (excluyendo Santiago)
- Después de completar la Fase 1, marca `tribunal_principal_procesado = true` para auditoría
- Permite priorizar los tribunales más consultados
- Implementación: `buscarPersona()` → `buscarEnTribunalesConFiltro()` → `buscarPorNombreParaleloConFiltro()`
- Flujo automático sin intervención del usuario
- Logs detallados de qué fase se está ejecutando

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

# Búsqueda completa
curl "http://localhost:8083/api/buscar-personas?archivo=personas.csv"
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

## 📁 Estructura de Datos

### Entrada (personas.csv)
```csv
NOMBRES,APELLIDO PATERNO,APELLIDO MATERNO,ANIO_INIT,ANIO_FIN
MIGUEL ANTONIO,SOTO,FREDES,2019,2024
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

## ⚙️ Configuración de Performance

### En PjudScraper.java
```java
private static final int MAX_THREADS = 3; // Máximo navegadores simultáneos
// Delay de 3 segundos entre aperturas (automático)
```

### Timeouts
- Conexión a BD: 30 segundos
- Shutdown: 60 segundos
- Navegación web: 60 segundos

## 🧪 Pruebas Rápidas

### Test 1: Validación de BD
```bash
# Detener BD
podman stop sqlserver

# Ejecutar app (debe fallar con error claro)
java -jar target/veritusbot-0.0.1-SNAPSHOT.jar
# Resultado: ❌ ERROR CRÍTICO: NO SE PUDO CONECTAR A LA BASE DE DATOS
```

### Test 2: Verificar Delays
```bash
# Ejecutar búsqueda (con años múltiples)
curl "http://localhost:8083/api/buscar-personas?archivo=personas.csv"

# En los logs, buscar:
# 📅 Año 2019 - Se iniciará en: 0 segundos
# 📅 Año 2020 - Se iniciará en: 3 segundos
# 📅 Año 2021 - Se iniciará en: 6 segundos
```

### Test 3: Máximo de Navegadores
```bash
# Ver navegadores activos durante búsqueda
pgrep -c chromium
# Resultado esperado: máximo 3
```

## 🐛 Troubleshooting

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

### "No se encontraron resultados"
- Es normal si la persona no existe en PJUD
- Verificar que personas.csv existe en la raíz
- Verificar que el formato es correcto
- Revisar los logs para más detalles

## 📈 Dependencias Principales

- **Spring Boot:** 3.2.5
- **Java:** 17
- **Microsoft Playwright:** 1.42.0
- **JSoup:** 1.17.2
- **Microsoft SQL Server JDBC:** Incluido en Spring
- **Maven:** 3.6+

## 🔐 Seguridad

- Credenciales SQL Server almacenadas en `application.properties`
- Para producción, usar variables de entorno
- Validación de entrada en formularios
- No hay exposición de datos sensibles en logs

## 📝 Logging

Los logs muestran:
- Inicio y fin de búsquedas
- Progreso por persona (cliente)
- Tiempo de ejecución
- Años procesados y delays
- Resultados encontrados
- Errores detallados si ocurren

Ejemplo:
```
╔════════════════════════════════════════════════════════════╗
║  INICIANDO BÚSQUEDA DE PERSONAS DESDE EXCEL                ║
║  Hora de inicio: 15:40:00                                  ║
╚════════════════════════════════════════════════════════════╝

📋 Personas a buscar: 1
  • PersonaDTO{nombres='MIGUEL ANTONIO', ...}

BUSCANDO PERSONA 1/1 | Progreso: 0.0%
📅 Año 2019 - Se iniciará en: 0 segundos
📅 Año 2020 - Se iniciará en: 3 segundos

✓ Total de causas para este cliente: 2
✓ CSV actualizado: resultados_busqueda.csv
✓ Base de datos actualizada
```

## 🚀 Deploy en Producción

### Requisitos
- Java 17+
- SQL Server 2022 o superior
- Acceso a internet (para oficinajudicialvirtual.pjud.cl)
- Al menos 4GB de RAM
- 2 cores de CPU

### Pasos
1. Compilar: `mvn clean package`
2. Copiar JAR a servidor
3. Configurar `application.properties` con credenciales reales
4. Asegurar que SQL Server está disponible
5. Ejecutar: `java -Xmx2G -jar veritusbot-0.0.1-SNAPSHOT.jar`

## 📞 Soporte

Para errores o preguntas:
1. Revisar logs (muy descriptivos)
2. Consultar esta documentación
3. Revisar archivo `compile.log` si hay errores de compilación

---

**Versión:** 2.0  
**Última actualización:** 25 Febrero 2026  
**Estado:** PRODUCTIVO ✅

## 🚀 Primeros Pasos

1. **Abre:** `SQL_SERVER_INTEGRATION.md` o `INICIO.md`
2. **Configura:** SQL Server con Podman (instrucciones en los .md)
3. **Crea:** BD y tablas (scripts en `SQL_SERVER_INTEGRATION.md`)
4. **Ejecuta:** `./mvnw spring-boot:run`

---

**¡El proyecto está 100% completado y listo para usar!**
