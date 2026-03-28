# 🔗 REFERENCIAS RÁPIDAS - Veritus Bot Autenticación

## 📌 Preguntas Respondidas

### Pregunta 1: ¿Puedo instalar esta app en Oracle Cloud Ubuntu?
**Respuesta:** ✅ **SÍ**, con configuración adecuada
- Instance Type: VM.Standard.E2.1 o superior
- SO: Ubuntu 20.04+ LTS recomendada
- **Ver:** `REQUISITOS_ORACLE_CLOUD.md`

### Pregunta 2: ¿Cuánta memoria RAM y CPU necesita?
**Respuesta:** ✅ **Documentado en detalle**
```
Desarrollo:      2 CPU, 4 GB RAM (mínimo)
Producción:      4 CPU, 8 GB RAM (recomendado)
Enterprise:      8 CPU, 16+ GB RAM (óptimo)
Total: 8-12 GB recomendado
```
**Ver:** `REQUISITOS_ORACLE_CLOUD.md`

### Pregunta 3: Crear endpoint /veritus-app/login
**Respuesta:** ✅ **Implementado completamente**
- Endpoint: `POST /api/veritus-app/login`
- Status: ✅ Compilado sin errores
- Flujo: Credenciales → Validación → JWT → Token
- **Ver:** `AUTENTICACION.md` y `GUIA_RAPIDA_LOGIN.md`

---

## 🚀 INICIO RÁPIDO

### 1. Compilar (30 segundos)
```bash
cd /Users/jellan/Documents/git/veritusbot
mvn clean compile
```
**Resultado esperado:** `BUILD SUCCESS`

### 2. Ejecutar (10 segundos)
```bash
mvn spring-boot:run
```
**Resultado esperado:** `Started on port 8083`

### 3. Probar Login (5 segundos)
```bash
curl -X POST http://localhost:8083/api/veritus-app/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@veritus.com","password":"admin123"}'
```
**Resultado esperado:** `{"token":"eyJ...","usuario":{...}}`

---

## 📁 ARCHIVOS IMPORTANTES

### Documentación
| Archivo | Propósito |
|---------|-----------|
| `AUTENTICACION.md` | Documentación técnica completa |
| `GUIA_RAPIDA_LOGIN.md` | Paso a paso para empezar |
| `REQUISITOS_ORACLE_CLOUD.md` | Instalación en Oracle Cloud |
| `RESUMEN_FINAL_COMPLETO.txt` | Documento consolidado |
| `README.md` | Información general del proyecto |

### Código
| Archivo | Descripción |
|---------|-----------|
| `LoginController.java` | Endpoint /api/veritus-app/login |
| `AuthenticationService.java` | Lógica de autenticación |
| `JwtTokenProvider.java` | Generación/validación de JWT |
| `SecurityConfig.java` | Configuración Spring Security |
| `Usuario.java` | Modelo de usuario |

### Scripts
| Archivo | Propósito |
|---------|-----------|
| `insert_test_usuarios.sql` | Crear usuarios de prueba |

---

## 🔐 USUARIOS DE PRUEBA

```
Email: admin@veritus.com
Contraseña: admin123
Rol: ADMIN

Email: operador@veritus.com
Contraseña: oper123
Rol: OPERADOR

Email: viewer@veritus.com
Contraseña: view123
Rol: VIEWER
```

---

## 📊 ESTADÍSTICAS

- **Archivos Java creados:** 20
- **Líneas de código:** ~2,500
- **Documentos creados:** 8
- **Status compilación:** ✅ BUILD SUCCESS
- **Archivos nuevos:** 23

---

## 🐛 PROBLEMAS COMUNES

| Problema | Solución |
|----------|----------|
| "Out of Memory" | Aumentar RAM o usar PostgreSQL |
| "Connection refused" | Verificar SQL Server corriendo |
| "Port 8083 in use" | Matar proceso anterior |
| "CORS error" | Actualizar app.cors.allowed-origins |
| "Token inválido" | Hacer nuevo login |

---

## 🔗 ENLACES RÁPIDOS

**Endpoints:**
- `POST /api/veritus-app/login` - Autenticar
- `POST /api/veritus-app/health-auth` - Health check
- `POST /api/buscar-personas` - Iniciar búsqueda (protegido)

**Configuración:**
- `application.properties` - Propiedades de la app
- `pom.xml` - Dependencias Maven

**Seguridad:**
- Spring Security: ✅ Habilitado
- JWT: ✅ Implementado (24h)
- BCrypt: ✅ Configurado (12 rounds)
- CORS: ✅ Configurado

---

## 💡 TIPS

1. **Para generar hashes BCrypt:**
   ```bash
   java -cp target/classes com.example.veritusbot.util.PasswordEncoderUtil "micontraseña"
   ```

2. **Para ver logs en tiempo real:**
   ```bash
   tail -f veritus.log
   ```

3. **Para verificar si Java está corriendo:**
   ```bash
   ps aux | grep java
   ```

4. **Para renovar token (próxima feature):**
   - Implementar endpoint `/refresh-token`
   - Usar refresh tokens adicionales

---

## ✅ CHECKLIST DE INSTALACIÓN

- [ ] Instalar Java 17
- [ ] Instalar Maven
- [ ] Clonar repositorio
- [ ] Ejecutar `mvn clean compile`
- [ ] Iniciar SQL Server o PostgreSQL
- [ ] Ejecutar `mvn spring-boot:run`
- [ ] Probar login con cURL
- [ ] Insertar usuarios de prueba
- [ ] Crear formulario en frontend
- [ ] Probar endpoints protegidos

---

## 🎯 PRÓXIMOS PASOS

### Hoy
- [ ] Leer GUIA_RAPIDA_LOGIN.md
- [ ] Compilar proyecto
- [ ] Probar endpoint

### Esta semana
- [ ] Frontend integration
- [ ] JWT en localStorage
- [ ] Endpoints protegidos

### Este mes
- [ ] Deploy en Oracle Cloud
- [ ] HTTPS + Dominio
- [ ] Refresh tokens

---

## 📞 SOPORTE

**Para dudas técnicas:**
- Ver `AUTENTICACION.md` (sección Troubleshooting)
- Ver `GUIA_RAPIDA_LOGIN.md` (sección FAQs)
- Ver `REQUISITOS_ORACLE_CLOUD.md` (sección Resolución de Problemas)

**Para código:**
- Revisar comentarios en archivos Java
- Consultar README.md del proyecto
- Ver ejemplos en documentación

---

## 📚 LECTURA RECOMENDADA (Orden)

1. Este archivo (REFERENCIAS_RAPIDAS.md) - 2 min
2. GUIA_RAPIDA_LOGIN.md - 10 min
3. AUTENTICACION.md - 20 min
4. REQUISITOS_ORACLE_CLOUD.md - 15 min
5. Código fuente (si necesario) - 30 min

**Tiempo total:** ~1 hora

---

**Versión:** 1.0
**Fecha:** 26 de Marzo, 2026
**Status:** ✅ Listo para usar

