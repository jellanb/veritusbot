# 🚀 INICIO RÁPIDO - VERITUS BOT

## 📌 En 5 Minutos

### 1️⃣ Asegúrate que SQL Server está corriendo
```bash
podman ps | grep sqlserver
```

Si no está, inicia:
```bash
podman run -d \
  --name sqlserver \
  -e "ACCEPT_EULA=Y" \
  -e "MSSQL_SA_PASSWORD=SqlServer2026Strong" \
  -p 14333:1433 \
  mcr.microsoft.com/mssql/server:2022-latest
```

### 2️⃣ Crea la tabla (si no existe)
```sql
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

### 3️⃣ Ejecuta el bot
```bash
cd /Users/jellan/Documents/git/veritusbot
java -jar target/veritusbot-0.0.1-SNAPSHOT.jar
```

### 4️⃣ Inicia búsqueda (otra terminal)
```bash
curl "http://localhost:8083/api/buscar-personas?archivo=personas.csv"
```

### 5️⃣ Verifica resultados
```sql
SELECT * FROM personas_procesadas;
SELECT COUNT(*) as causas FROM causas;
SELECT * FROM resultados_busqueda.csv;
```

---

## 📊 Cambiar Nivel de Logs

### Ver TODO (modo desarrollo)
```ini
# En application.properties
app.environment=desarrollo
```

### Solo INFO/WARNING/ERROR (modo pruebas)
```ini
app.environment=pruebas
```

### Solo WARNING/ERROR (modo producción)
```ini
app.environment=produccion
```

---

## 🔍 Monitorear Búsqueda

### En otra terminal, ver progreso de BD
```sql
-- Ver personas siendo procesadas
SELECT * FROM personas_procesadas WHERE procesado = 0;

-- Ver personas completadas
SELECT * FROM personas_procesadas WHERE procesado = 1;

-- Ver causas encontradas
SELECT COUNT(*) FROM causas;

-- Ver progreso de recuperación
SELECT * FROM search_progress.json;
```

---

## ⚙️ Ajustes Útiles

### Cambiar cantidad de navegadores simultáneos
En `PjudScraper.java`:
```java
private static final int MAX_THREADS = 2; // Cambiar aquí (máximo 3)
```

### Cambiar delay entre navegadores
En `PjudScraper.java`:
```java
currentDelay += 5; // Cambiar aquí (segundos)
```

### Cambiar puerto del servidor
En `application.properties`:
```properties
server.port=8084  # O el puerto que quieras
```

---

## 🐛 Troubleshooting Rápido

### Bot no inicia
```
Verifica que SQL Server esté corriendo: podman ps | grep sqlserver
```

### Base de datos no conecta
```
Verifica usuario/contraseña en application.properties
Verifica puerto: 14333
```

### Tabla no existe
```
Ejecuta el SQL CREATE TABLE de arriba
```

### Logs muy silenciosos
```
Cambiar app.environment=desarrollo en application.properties
```

### Logs muy verbosos
```
Cambiar app.environment=produccion en application.properties
```

---

## 📋 Archivos Importante

| Archivo | Propósito |
|---------|-----------|
| `personas.csv` | Entrada (personas a buscar) |
| `resultados_busqueda.csv` | Salida (causas encontradas) |
| `search_progress.json` | Estado (para recuperación) |
| `application.properties` | Configuración |

---

## 🎯 Resultado Esperado

### Archivos generados
```
✅ resultados_busqueda.csv (con causas encontradas)
✅ search_progress.json (temporal, durante búsqueda)
```

### Datos en BD
```
✅ Tabla personas: personas buscadas
✅ Tabla causas: resultados encontrados
✅ Tabla personas_procesadas: personas con estado
```

### Logs
```
✅ Guardando personas en tabla personas_procesadas...
✅ Buscando persona 1/X...
✅ Marcada como procesada: NOMBRE APELLIDO
✅ Búsqueda completada
```

---

## ⏱️ Tiempo Estimado

- **Búsqueda de 1 persona en 1 año, 1 tribunal:** 2-5 minutos
- **Búsqueda de 1 persona en 1 año, todos tribunales:** 20-40 minutos
- **Búsqueda de 1 persona en 6 años, todos tribunales:** 120-240 minutos
- **Búsqueda de 3 personas en 2 años, todos tribunales:** 180-360 minutos

---

## 💡 Tips

- Usa `app.environment=desarrollo` para ver qué está pasando
- Si algo falla, revisa `search_progress.json` para ver dónde se quedó
- Los logs muestran "Reanudando desde tribunal X" si recupera
- Verifica tabla `personas_procesadas` para ver progreso

---

## ✅ Listo

El bot está compilado y listo para usar. Solo ejecuta los 5 pasos de arriba.

**¡Listo para buscar! 🚀**

