# 🚀 Guía Rápida - Sistema de Autenticación

## Requisitos Previos

✅ Java 17+
✅ Maven 3.8+
✅ SQL Server corriendo en localhost:14333
✅ Base de datos `veritus` creada

---

## 1️⃣ Compilar el Proyecto

```bash
cd /Users/jellan/Documents/git/veritusbot
mvn clean compile
```

**Resultado esperado:** `BUILD SUCCESS`

---

## 2️⃣ Crear Base de Datos de Usuarios

### Opción A: Automático (Recomendado)
JPA/Hibernate crea la tabla `usuarios` automáticamente al iniciar la aplicación.

### Opción B: Manual (SQL Server Management Studio)

1. Abrir SQL Server Management Studio
2. Conectarse a: `localhost,14333`
3. Ejecutar el script: `/Users/jellan/Documents/git/veritusbot/insert_test_usuarios.sql`

---

## 3️⃣ Crear Usuarios de Prueba

### Generar Hashes BCrypt

```bash
cd /Users/jellan/Documents/git/veritusbot
mvn clean compile

# Generar hash para "admin123"
java -cp target/classes com.example.veritusbot.util.PasswordEncoderUtil "admin123"
```

**Salida:**
```
Contraseña: admin123
Hash BCrypt: $2a$12$XWX4cKK75LqLV3jJXuUG5ejPVU.H9hYxuYX7ZuUPx4p/WngVqGdQ.
```

### Insertar Usuarios en BD

```sql
-- En SQL Server Management Studio

-- Usuario 1: Admin
INSERT INTO usuarios (id, email, password_hash, nombre_completo, rol, estado)
VALUES (
    NEWID(),
    'admin@veritus.com',
    '$2a$12$XWX4cKK75LqLV3jJXuUG5ejPVU.H9hYxuYX7ZuUPx4p/WngVqGdQ.',
    'Administrador',
    'ADMIN',
    'ACTIVO'
);
```

---

## 4️⃣ Iniciar la Aplicación

```bash
# Opción 1: Ejecutar con Maven
mvn spring-boot:run

# Opción 2: Desde IDE (IntelliJ IDEA)
- Click derecho en VeritusbotApplication.java → Run
```

**Resultado esperado:**
```
[INFO] Started VeritusbotApplication in 8.5 seconds
Tomcat started on port(s): 8083
```

---

## 5️⃣ Probar Login

### Con cURL:
```bash
curl -X POST http://localhost:8083/api/veritus-app/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@veritus.com",
    "password": "admin123"
  }'
```

### Con Postman:
1. **Método:** POST
2. **URL:** `http://localhost:8083/api/veritus-app/login`
3. **Headers:** `Content-Type: application/json`
4. **Body:**
```json
{
  "email": "admin@veritus.com",
  "password": "admin123"
}
```

### Respuesta Esperada (200 OK):
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "usuario": {
    "id": "abc123...",
    "email": "admin@veritus.com",
    "nombreCompleto": "Administrador",
    "rol": "ADMIN",
    "estado": "ACTIVO"
  },
  "loginAt": "2026-03-26T19:05:32"
}
```

---

## 6️⃣ Probar Endpoint Protegido

### Con cURL:
```bash
# Primero obtener el token
TOKEN=$(curl -s -X POST http://localhost:8083/api/veritus-app/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@veritus.com","password":"admin123"}' | jq -r '.token')

# Usar el token en un endpoint protegido
curl -X POST http://localhost:8083/api/buscar-personas?archivo=personas.csv \
  -H "Authorization: Bearer $TOKEN"
```

### Con Postman:
1. **Método:** POST
2. **URL:** `http://localhost:8083/api/buscar-personas?archivo=personas.csv`
3. **Headers:**
   - `Authorization: Bearer <TOKEN>`
   - `Content-Type: application/json`
4. **Click Send**

---

## 📊 Usuarios de Prueba por Defecto

| Email | Contraseña | Rol | Estado |
|-------|-----------|-----|--------|
| admin@veritus.com | admin123 | ADMIN | ACTIVO |
| operador@veritus.com | oper123 | OPERADOR | ACTIVO |
| viewer@veritus.com | view123 | VIEWER | ACTIVO |

---

## 🔍 Verificar Usuarios en BD

```sql
SELECT id, email, nombre_completo, rol, estado, last_login 
FROM usuarios 
ORDER BY created_at DESC;
```

---

## 🛠️ Troubleshooting

### Error: "Usuario no encontrado"
- ✅ Verificar que la contraseña sea correcta
- ✅ Verificar que el usuario existe en BD: `SELECT * FROM usuarios`
- ✅ Revisar que el email sea exacto

### Error: "Token inválido o expirado"
- ✅ Hacer login nuevamente
- ✅ Verificar formato: `Authorization: Bearer <token>`
- ✅ Token expira en 24 horas

### Error: CORS
- ✅ Verificar `app.cors.allowed-origins` en `application.properties`
- ✅ Frontend debe estar en puerto permitido (3000, 4200, 8080, etc.)

### Error: Database connection failed
- ✅ Verificar SQL Server está corriendo: `netstat -an | grep 14333`
- ✅ Verificar credenciales en `application.properties`
- ✅ Verificar base de datos `veritus` existe

---

## 📚 Documentación Completa

Ver archivo: `/Users/jellan/Documents/git/veritusbot/AUTENTICACION.md`

---

## ✅ Checklist de Configuración

- [ ] SQL Server corriendo en localhost:14333
- [ ] Base de datos `veritus` existe
- [ ] Tabla `usuarios` creada (automático o manual)
- [ ] Usuarios de prueba insertados
- [ ] `mvn clean compile` ejecutado exitosamente
- [ ] Aplicación iniciada (`mvn spring-boot:run`)
- [ ] Login probado con cURL/Postman
- [ ] Token recibido correctamente
- [ ] Endpoint protegido accesible con token

---

**¡Listo para usar el sistema de autenticación! 🎉**

