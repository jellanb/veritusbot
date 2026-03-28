# 💻 Requisitos de Sistema - Veritus Bot

## 1️⃣ REQUISITOS MÍNIMOS

### Desarrollo (Localhost)
```
CPU:    2 cores (mínimo)
RAM:    4 GB (mínimo para Java + SQL Server)
Disco:  10 GB (SSD recomendado)
OS:     Windows, macOS, Linux
```

### Producción (Servidor)
```
CPU:    4+ cores (recomendado)
RAM:    8 GB (mínimo), 16 GB+ (recomendado)
Disco:  50 GB SSD (para logs y datos)
OS:     Linux (Ubuntu 20.04+ recomendado)
```

---

## 2️⃣ DESGLOSE DE MEMORIA

### Java Spring Boot Application
```
Heap Memory:    512 MB - 2 GB (configurable)
Non-Heap:       200 MB (metaspace, code cache)
Threads:        3 (pool por defecto)
─────────────────────────
Total Java:     ~1 GB en reposo
                ~2 GB bajo carga
```

### SQL Server (Contenedor Docker)
```
Mínimo requerido:   2 GB (como sale en los logs)
Recomendado:        4+ GB
─────────────────────────
Total SQL Server:   ~4 GB
```

### Sistema Operativo
```
Ubuntu kernel:      300-500 MB
Sistema:            500-700 MB
─────────────────────────
Total OS:           ~1 GB
```

### **TOTAL RECOMENDADO: 8-12 GB RAM**

---

## 3️⃣ PROBLEMA CON SQL SERVER EN ORACLE CLOUD

### ❌ Lo que viste en los logs:
```
sqlservr: This program requires a machine with at least 2000 megabytes of memory.
```

### 🔴 Causa:
SQL Server 2022 en Docker necesita **mínimo 2 GB de RAM dedicada**. Oracle Cloud con plan gratuito típicamente ofrece:
- **Tipo A1 Compute (Ampere):** 1 vCPU + 1 GB RAM ❌ INSUFICIENTE
- **Tipo A1 Compute (E4 Flex):** 2 vCPU + 2-4 GB RAM ✅ MÍNIMO FUNCIONAL

---

## 4️⃣ SOLUCIÓN PARA ORACLE CLOUD

### Opción A: Usar Tier Suficiente (Recomendado)
```
Instance Type:      VM.Standard.E2.1.Micro o superior
vCPU:               2+
Memory:             4 GB+
Monthly Cost:       USD 9-20 (siempre dentro free tier en algunos casos)
```

### Opción B: Lightweight SQL (Alternativa)
```
En lugar de SQL Server 2022 en Docker:
  • PostgreSQL (mucho más ligero)
  • MySQL 8
  • MariaDB
  
Memoria requerida:  500 MB - 1 GB
```

### Opción C: SQL Server en Contenedor con Límite de Memoria
```bash
podman run -d \
  --name sqlserver \
  --memory=1.5g \
  --memory-swap=2g \
  -e "ACCEPT_EULA=Y" \
  -e "MSSQL_SA_PASSWORD=SqlServer!2026#" \
  -e "MSSQL_MEMORY_LIMIT_MB=768" \
  -p 14333:1433 \
  mcr.microsoft.com/mssql/server:2022-latest
```

---

## 5️⃣ INSTALACIÓN EN ORACLE CLOUD UBUNTU

### Paso 1: Crear Instancia
```bash
# Oracle Cloud Console:
# 1. Compute → Instances → Create Instance
# 2. Image: Ubuntu 22.04 LTS
# 3. Shape: VM.Standard.E2.1 (2 vCPU, 4 GB RAM)
#    o VM.Standard.E2.1.Micro si free tier
# 4. VCN/Subnet: Default
# 5. Asignar IP Pública
# 6. Descargar SSH Key
# 7. Create
```

### Paso 2: Conectarse a la Instancia
```bash
# En tu máquina local:
chmod 600 /path/to/ssh-key.key
ssh -i /path/to/ssh-key.key ubuntu@<PUBLIC_IP>
```

### Paso 3: Instalar Prerequisites
```bash
# Actualizar sistema
sudo apt update && sudo apt upgrade -y

# Instalar Java 17
sudo apt install -y openjdk-17-jdk-headless
java -version  # Verificar

# Instalar Maven
sudo apt install -y maven
mvn -version   # Verificar

# Instalar Git
sudo apt install -y git
git --version  # Verificar

# Instalar Docker o Podman
sudo apt install -y podman
podman --version  # Verificar
```

### Paso 4: Clonar Repositorio
```bash
cd /home/ubuntu
git clone https://github.com/tu-usuario/veritusbot.git
cd veritusbot
```

### Paso 5: Iniciar SQL Server
```bash
# ⚠️ Si tienes 2 GB RAM, usar límite de memoria
podman run -d \
  --name sqlserver \
  --memory=1.5g \
  -e "ACCEPT_EULA=Y" \
  -e "MSSQL_SA_PASSWORD=SqlServer!2026#" \
  -e "MSSQL_MEMORY_LIMIT_MB=768" \
  -p 14333:1433 \
  mcr.microsoft.com/mssql/server:2022-latest

# Esperar a que inicie (30-60 segundos)
sleep 45

# Verificar que está corriendo
podman ps | grep sqlserver
```

### Paso 6: Actualizar application.properties
```bash
# Si SQL Server está en mismo host:
# application.properties ya tiene localhost:14333 ✅

# Si está en otro host:
sudo nano src/main/resources/application.properties
# Cambiar:
# spring.datasource.url=jdbc:sqlserver://<NUEVA_IP>:14333;...
```

### Paso 7: Compilar y Ejecutar
```bash
# Compilar
mvn clean compile

# Ejecutar en background
nohup mvn spring-boot:run > veritus.log 2>&1 &

# O con límite de memoria si RAM es limitado
nohup java -Xmx1g -Xms512m -jar target/veritusbot-0.0.1-SNAPSHOT.jar > veritus.log 2>&1 &

# Verificar que está corriendo
ps aux | grep java
tail -f veritus.log
```

### Paso 8: Configurar Firewall (Security Lists)
```bash
# En Oracle Cloud Console:
# 1. Networking → Virtual Cloud Networks
# 2. Seleccionar VCN
# 3. Security Lists → Default
# 4. Add Ingress Rule:
#    - Source: 0.0.0.0/0 (o tu IP específica)
#    - Protocol: TCP
#    - Destination Port: 8083
# 5. Add Rule
```

### Paso 9: Probar Acceso
```bash
# Desde tu máquina local:
curl -X POST http://<ORACLE_PUBLIC_IP>:8083/api/veritus-app/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@veritus.com","password":"admin123"}'

# Debería retornar el token JWT
```

---

## 6️⃣ OPTIMIZACIONES PARA ORACLE CLOUD CON RAM LIMITADA

### Reducir Heap de Java
```bash
# Si tienes 2 GB RAM total:
export JAVA_OPTS="-Xmx512m -Xms256m"
mvn spring-boot:run
```

### Configurar Thread Pool Menor
```properties
# En application.properties:
spring.task.execution.pool.core-size=1
spring.task.execution.pool.max-size=2
spring.task.execution.pool.queue-capacity=50
```

### SQL Server con Menos Memoria
```bash
podman run -d \
  --name sqlserver \
  --memory=1g \
  -e "ACCEPT_EULA=Y" \
  -e "MSSQL_SA_PASSWORD=SqlServer!2026#" \
  -e "MSSQL_MEMORY_LIMIT_MB=512" \
  -p 14333:1433 \
  mcr.microsoft.com/mssql/server:2022-latest
```

### Monitorear Memoria
```bash
# En tiempo real:
watch -n 1 free -h

# Detallado:
podman stats sqlserver
ps aux --sort=-%mem | head -10
```

---

## 7️⃣ ALTERNATIVA: USAR POSTGRESQL EN LUGAR DE SQL SERVER

### Si Oracle Cloud es muy limitado:
```bash
# Instalar PostgreSQL (mucho más ligero)
sudo apt install -y postgresql postgresql-contrib

# Iniciar servicio
sudo systemctl start postgresql
sudo systemctl enable postgresql

# Crear base de datos
sudo -u postgres createdb veritus
sudo -u postgres psql -c "ALTER USER postgres PASSWORD 'postgres123';"
```

### Cambiar en pom.xml y application.properties
```xml
<!-- Cambiar de SQL Server a PostgreSQL -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
</dependency>
```

```properties
# Cambiar la URL
spring.datasource.url=jdbc:postgresql://localhost:5432/veritus
spring.datasource.username=postgres
spring.datasource.password=postgres123
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
```

**Ventaja:** PostgreSQL requiere ~200 MB RAM vs 2 GB de SQL Server

---

## 8️⃣ COMPARATIVA DE RECURSOS

| Sistema | CPU | RAM | Disco | Costo (Oracle) |
|---------|-----|-----|-------|---|
| **Mínimo (Dev)** | 2 | 4 GB | 20 GB | Free/USD 9 |
| **Recomendado** | 4 | 8 GB | 50 GB | USD 20-30 |
| **Producción** | 8 | 16 GB | 100 GB | USD 50-100 |

---

## 9️⃣ MONITOREO EN PRODUCCIÓN

### Script de Health Check
```bash
#!/bin/bash
# health-check.sh

# Verificar Java
if ! pgrep -f "java" > /dev/null; then
    echo "❌ Java process not running!"
    exit 1
fi

# Verificar SQL Server
if ! pgrep -f "sqlserver" > /dev/null; then
    echo "❌ SQL Server not running!"
    exit 1
fi

# Verificar endpoint
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8083/api/veritus-app/health-auth)
if [ "$RESPONSE" != "200" ]; then
    echo "❌ API not responding. Status: $RESPONSE"
    exit 1
fi

echo "✅ All systems operational"
```

### Ejecutar periódicamente
```bash
# Cron job cada 5 minutos
*/5 * * * * /home/ubuntu/health-check.sh >> /var/log/veritus-health.log 2>&1
```

---

## 🔟 TROUBLESHOOTING EN ORACLE CLOUD

### Error: "Out of Memory"
```
Solución: 
  1. Aumentar RAM de instancia
  2. Reducir JAVA_OPTS -Xmx
  3. Usar PostgreSQL en lugar de SQL Server
```

### Error: "Connection refused"
```
Solución:
  1. Verificar que SQL Server esté corriendo: podman ps
  2. Verificar firewall: sudo ufw allow 14333
  3. Verificar security lists en Oracle Cloud
```

### Error: "Port 8083 in use"
```
Solución:
  # Encontrar proceso usando puerto
  sudo lsof -i :8083
  
  # Matar proceso
  sudo kill -9 <PID>
```

---

## 📊 RECOMENDACIÓN FINAL PARA ORACLE CLOUD

| Caso | Solución | Costo |
|------|----------|-------|
| **Desarrollo** | VM.Standard.E2.1.Micro + PostgreSQL | Free tier |
| **Pruebas** | VM.Standard.E2.1 + SQL Server | ~USD 9-20 |
| **Producción** | VM.Standard.E2.2 + SQL Server | ~USD 30-50 |

**Mi recomendación:** 
- Para empezar: PostgreSQL en VM.Standard.E2.1.Micro (free tier)
- Luego: Migrar a SQL Server si es necesario

---

**Versión:** 1.0
**Última actualización:** 26 Marzo, 2026
**Status:** ✅ Listo para Oracle Cloud

