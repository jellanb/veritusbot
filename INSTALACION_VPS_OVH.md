# Instalacion y ejecucion de VeritusBot en VPS OVH (Ubuntu 22.04)

## Objetivo
Esta guia resume el proceso real que seguimos para dejar la app funcionando en un VPS de OVH con Ubuntu 22.04, incluyendo errores que ocurrieron y como resolverlos rapido.

## 1) Conexion inicial al VPS

### Comando correcto de SSH
Usa el usuario del servidor, no el hostname como usuario.

```bash
ssh root@TU_IP_VPS
```

Ejemplo de error que tuvimos:
- Se uso `ssh vps-xxxx@IP` (usuario incorrecto), y devolvia `Permission denied`.

### Nota importante al escribir password
En terminal no se ve nada al escribir/pegar la contrasena. Es normal.

---

## 2) Paquetes base en Ubuntu 22.04

```bash
sudo apt update
sudo apt install -y git curl ca-certificates openjdk-17-jdk maven podman xvfb ufw
```

Verificacion:

```bash
java -version
mvn -version
podman --version
```

---

## 3) Obtener codigo en el VPS

```bash
cd /opt
sudo git clone <URL_DEL_REPO> veritusbot
cd /opt/veritusbot
```

Si el repositorio quedo con permisos de root y tu usuario no puede escribir logs/builds:

```bash
sudo chown -R $USER:$USER /opt/veritusbot
```

Error real que vimos:
- `app.log: Permission denied`
- Causa: permisos del directorio `/opt/veritusbot`.

---

## 4) Levantar SQL Server (requerido por la app)

La app esta configurada para conectarse a:
- host: `localhost`
- puerto: `14333`
- BD: `veritus`
- usuario: `sa`

Comando usado:

```bash
sudo podman run -d \
--name sqlserver \
--restart unless-stopped \
-e ACCEPT_EULA=Y \
-e MSSQL_SA_PASSWORD='SqlServer!2026#' \
-e MSSQL_MEMORY_LIMIT_MB=1024 \
-p 14333:1433 \
mcr.microsoft.com/mssql/server:2022-latest
```

Verificar contenedor:

```bash
sudo podman ps
sudo podman logs --tail 50 sqlserver
```

### Error real que vimos
`Cannot open database "veritus" requested by the login.`

Causa:
- SQL Server estaba arriba, pero la base `veritus` no existia aun.

Solucion:

```bash
sudo podman exec -it sqlserver /opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P 'SqlServer!2026#'
```

Si no existe esa ruta, usar:

```bash
sudo podman exec -it sqlserver /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P 'SqlServer!2026#' -C
```

Dentro de `sqlcmd`:

```sql
CREATE DATABASE veritus;
GO
USE veritus;
GO
EXIT
```

---

## 5) Compilar la app

```bash
cd /opt/veritusbot
chmod +x mvnw
./mvnw clean package -DskipTests
```

Verificar jar:

```bash
ls -lah target/veritusbot-0.0.1-SNAPSHOT.jar
```

### Error real que vimos
`Unable to access jarfile target/veritusbot-0.0.1-SNAPSHOT.jar`

Causa:
- Se intento ejecutar antes de compilar.

Solucion:
- Ejecutar `./mvnw clean package -DskipTests` primero.

---

## 6) Instalar Chromium para Playwright

```bash
cd /opt/veritusbot
./mvnw -DskipTests org.codehaus.mojo:exec-maven-plugin:3.5.0:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install --with-deps chromium"
```

Verificar descarga:

```bash
ls -lah ~/.cache/ms-playwright
```

### Pantalla morada "Daemons using outdated libraries"
Durante instalacion de dependencias puede aparecer `needrestart`.

Que hacer:
- Dejar seleccion por defecto.
- Ir a `<Ok>` y presionar Enter.

---

## 7) Ejecucion manual de la app

Como el codigo lanza Chromium en modo no headless (`headful`), en VPS sin entorno grafico se debe usar `xvfb-run`.

```bash
cd /opt/veritusbot
nohup xvfb-run -a java -jar target/veritusbot-0.0.1-SNAPSHOT.jar > app.log 2>&1 &
```

Ver logs:

```bash
tail -f /opt/veritusbot/app.log
```

Pruebas basicas:

```bash
curl -X POST http://127.0.0.1:8083/api/veritus-app/health-auth
curl -X POST http://127.0.0.1:8083/api/veritus-app/login -H "Content-Type: application/json" -d '{"email":"admin@veritus.com","password":"admin123"}'
```

---

## 8) Detener la app (modo manual)

Buscar PID y detener limpio:

```bash
ps -ef | grep "veritusbot-0.0.1-SNAPSHOT.jar" | grep -v grep
kill <PID>
```

Forzar (solo si no responde):

```bash
kill -9 <PID>
```

Atajo:

```bash
pkill -f "veritusbot-0.0.1-SNAPSHOT.jar"
```

---

## 9) Ejecutar como servicio systemd (recomendado)

Crear servicio:

```bash
sudo tee /etc/systemd/system/veritusbot.service > /dev/null <<'EOF'
[Unit]
Description=Veritus Bot Spring Boot Service
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
User=ubuntu
WorkingDirectory=/opt/veritusbot
ExecStart=/usr/bin/xvfb-run -a /usr/bin/java -jar /opt/veritusbot/target/veritusbot-0.0.1-SNAPSHOT.jar
SuccessExitStatus=143
Restart=on-failure
RestartSec=5
Environment=JAVA_OPTS=-Xms512m -Xmx1024m

[Install]
WantedBy=multi-user.target
EOF
```

Activar y arrancar:

```bash
sudo systemctl daemon-reload
sudo systemctl enable veritusbot
sudo systemctl start veritusbot
sudo systemctl status veritusbot --no-pager -l
```

Logs del servicio:

```bash
sudo journalctl -u veritusbot -f
```

Operaciones diarias:

```bash
sudo systemctl stop veritusbot
sudo systemctl start veritusbot
sudo systemctl restart veritusbot
sudo systemctl status veritusbot
```

---

## 10) Ajustar logs para diagnostico (INFO global + DEBUG en Phase1/Phase2)

Nota importante:
- `app.environment=desarrollo` no siempre alcanza para ver detalle de clases con `SLF4J`.
- Para diagnosticar avance real, conviene fijar `logging.level` desde `systemd`.

Aplicar override de logs:

```bash
sudo mkdir -p /etc/systemd/system/veritusbot.service.d
sudo tee /etc/systemd/system/veritusbot.service.d/logging.conf > /dev/null <<'EOF'
[Service]
Environment="JAVA_TOOL_OPTIONS=-Dlogging.level.root=INFO -Dlogging.level.com.example.veritusbot.service.scraper.phases.Phase1Scraper=DEBUG -Dlogging.level.com.example.veritusbot.service.scraper.phases.Phase2Scraper=DEBUG"
EOF
sudo systemctl daemon-reload
sudo systemctl restart veritusbot
sudo journalctl -u veritusbot -f
```

Verificar que el override quedo activo:

```bash
sudo systemctl show veritusbot -p Environment
```

Opcional: volver a nivel normal

```bash
sudo rm -f /etc/systemd/system/veritusbot.service.d/logging.conf
sudo systemctl daemon-reload
sudo systemctl restart veritusbot
```

---

## 11) Lecciones aprendidas (errores reales)

1. **Usuario SSH correcto**: `usuario@host` debe ser un usuario real del servidor (`root`, `ubuntu`, etc).
2. **Permisos en `/opt`**: si clonaste con `sudo`, probablemente necesites `chown` para poder generar logs y artefactos.
3. **No hay JAR sin build**: siempre compilar antes de ejecutar.
4. **SQL Server != base creada**: aunque el contenedor este corriendo, hay que crear la BD `veritus`.
5. **Playwright en VPS**: si Chromium corre en modo visual, necesitas `xvfb-run`.
6. **Pantalla `needrestart`**: no es bloqueo fatal; se continua con `<Ok>`.
7. **Visibilidad de avance**: para ver progreso por tribunal en `Phase1Scraper`/`Phase2Scraper`, ajustar `logging.level` (no solo `app.environment`).

---

## 12) Checklist rapido de validacion final

```bash
sudo podman ps
ls -lah /opt/veritusbot/target/veritusbot-0.0.1-SNAPSHOT.jar
sudo systemctl status veritusbot --no-pager -l
curl -X POST http://127.0.0.1:8083/api/veritus-app/health-auth
```

Si todo lo anterior responde bien, la app quedo instalada y operativa en el VPS.

