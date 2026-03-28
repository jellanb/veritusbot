# 🔐 Sistema de Autenticación - Veritus Bot

## Resumen de Cambios Implementados

Se ha implementado un sistema completo de autenticación y autorización basado en **JWT (JSON Web Tokens)** y **Spring Security** para proteger los endpoints de la aplicación.

---

## 📋 Componentes Creados

### 1. **Modelos de Datos**
- `Usuario.java` - Entidad principal de usuarios
- `RolUsuario.java` - Enum con roles: ADMIN, OPERADOR, VIEWER
- `EstadoUsuario.java` - Enum con estados: ACTIVO, INACTIVO, BLOQUEADO

### 2. **DTOs (Data Transfer Objects)**
- `LoginRequestDTO.java` - Recibe credenciales (email, password)
- `LoginResponseDTO.java` - Retorna token y datos del usuario
- `UsuarioDTO.java` - Información pública del usuario (sin contraseña)
- `ErrorResponseDTO.java` - Respuestas de error estandarizadas

### 3. **Excepciones Personalizadas**
- `UsuarioNoEncontradoException.java`
- `ContrasenaInvalidaException.java`
- `UsuarioBloqueadoException.java`
- `TokenInvalidoException.java`

### 4. **Servicios**
- `AuthenticationService.java` - Lógica de autenticación
- `JwtTokenProvider.java` - Generación y validación de tokens JWT

### 5. **Controladores**
- `LoginController.java` - Endpoint `/api/veritus-app/login`

### 6. **Seguridad**
- `SecurityConfig.java` - Configuración de Spring Security
- `SecurityBeansConfig.java` - Configuración de beans (PasswordEncoder, CORS)
- `JwtAuthenticationFilter.java` - Filtro para validar JWT en cada request

### 7. **Repositorio**
- `UsuarioRepository.java` - Acceso a datos de usuarios

---

## 🚀 Endpoints Disponibles

### 1. **Login** (Público)
```http
POST /api/veritus-app/login
Content-Type: application/json

{
  "email": "usuario@example.com",
  "password": "contraseña123"
}
```

**Respuesta (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJjYmZmOGE3Mi1hNTA3LTQzZWQtYjNhYy1lOGQ1YmQ0MzAyNTMiLCJlbWFpbCI6ImFkbWluQHZlcml0dXMuY29tIiwicm9sIjoiQURNSU4iLCJub21icmVDb21wbGV0byI6IkFkbWluaXN0cmFkb3IiLCJpYXQiOjE3MTExNDcyMzIsImV4cCI6MTcxMTIzMzYzMn0.abc123...",
  "usuario": {
    "id": "cbff8a72-a507-43ed-b3ac-e8d5bd430253",
    "email": "admin@veritus.com",
    "nombreCompleto": "Administrador Sistema",
    "rol": "ADMIN",
    "estado": "ACTIVO",
    "createdAt": "2026-03-26T19:00:00",
    "lastLogin": "2026-03-26T19:05:30"
  },
  "loginAt": "2026-03-26T19:05:32",
  "mensaje": "Login exitoso"
}
```

**Respuesta (401 Unauthorized):**
```json
{
  "mensaje": "Email o contraseña incorrectos",
  "codigo": "USUARIO_NO_ENCONTRADO",
  "timestamp": "2026-03-26T19:05:32"
}
```

### 2. **Health Check** (Público)
```http
POST /api/veritus-app/health-auth
```

---

## 🔒 Rutas Protegidas (Requieren JWT)

### Búsqueda de Personas
```http
POST /api/buscar-personas?archivo=personas.csv
Authorization: Bearer <JWT_TOKEN>
```

**El token debe incluirse en el header `Authorization`:**
```
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWI...
```

---

## 📦 Dependencias Agregadas

```xml
<!-- Spring Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- JWT (JJWT) -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.3</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>
```

---

## ⚙️ Configuración (application.properties)

```properties
# JWT Configuration
app.jwt.secret=tusecretkeysuper1seguro2minimo3256bits4parahs512
app.jwt.expiration=86400000  # 24 horas en milisegundos

# CORS Configuration
app.cors.allowed-origins=http://localhost:3000,http://localhost:4200,http://localhost:8080
```

⚠️ **IMPORTANTE PARA PRODUCCIÓN:**
- Cambiar `app.jwt.secret` por una clave segura de al menos 256 bits
- Actualizar `app.cors.allowed-origins` con dominios reales
- Usar variables de entorno para datos sensibles

---

## 🗄️ Base de Datos - Tabla Usuarios

La tabla se crea automáticamente mediante JPA/Hibernate. Estructura:

```sql
CREATE TABLE usuarios (
    id UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(),
    email NVARCHAR(255) NOT NULL UNIQUE,
    password_hash NVARCHAR(255) NOT NULL,
    nombre_completo NVARCHAR(255) NULL,
    rol NVARCHAR(50) NOT NULL DEFAULT 'USER',
    estado NVARCHAR(50) NOT NULL DEFAULT 'ACTIVO',
    created_at DATETIME NOT NULL DEFAULT GETDATE(),
    updated_at DATETIME NOT NULL DEFAULT GETDATE(),
    last_login DATETIME NULL
);

CREATE INDEX idx_usuarios_email ON usuarios(email);
CREATE INDEX idx_usuarios_estado ON usuarios(estado);
CREATE INDEX idx_usuarios_rol ON usuarios(rol);
```

---

## 👥 Crear Usuarios de Prueba

### Paso 1: Generar Hash BCrypt

Usar la utilidad `PasswordEncoderUtil.java`:

```bash
# Durante desarrollo, ejecutar desde IDE o compilar:
cd /Users/jellan/Documents/git/veritusbot

# Compilar el proyecto
mvn clean compile

# Generar hash para contraseña "admin123"
java -cp target/classes:~/.m2/repository/org/springframework/security/spring-security-crypto/6.0.2/spring-security-crypto-6.0.2.jar:~/.m2/repository/org/springframework/security/spring-security-core/6.0.2/spring-security-core-6.0.2.jar com.example.veritusbot.util.PasswordEncoderUtil "admin123"
```

Salida esperada:
```
Contraseña: admin123
Hash BCrypt: $2a$12$abcdefghijklmnopqrstuvwxyz1234567890abcdefghijklmno
```

### Paso 2: Insertar en BD

```sql
INSERT INTO usuarios (id, email, password_hash, nombre_completo, rol, estado)
VALUES (
    NEWID(),
    'admin@veritus.com',
    '$2a$12$...',  -- Hash generado en paso anterior
    'Administrador Sistema',
    'ADMIN',
    'ACTIVO'
);

INSERT INTO usuarios (id, email, password_hash, nombre_completo, rol, estado)
VALUES (
    NEWID(),
    'operador@veritus.com',
    '$2a$12$...',
    'Operador Sistema',
    'OPERADOR',
    'ACTIVO'
);

INSERT INTO usuarios (id, email, password_hash, nombre_completo, rol, estado)
VALUES (
    NEWID(),
    'viewer@veritus.com',
    '$2a$12$...',
    'Visualizador Sistema',
    'VIEWER',
    'ACTIVO'
);
```

---

## 🎯 Flujo de Autenticación

```
1. Usuario ingresa credenciales en frontend
   ↓
2. POST /api/veritus-app/login {email, password}
   ↓
3. AuthenticationService valida:
   - Email existe en BD
   - Contraseña coincide (BCrypt)
   - Usuario está activo
   - Usuario no está bloqueado
   ↓
4. JwtTokenProvider genera JWT
   - Subject: UUID del usuario
   - Claims: email, rol, nombreCompleto
   - Expiration: 24 horas
   ↓
5. LoginResponseDTO retorna:
   - token (JWT)
   - usuario (DTO)
   - loginAt (timestamp)
   ↓
6. Frontend guarda token en localStorage/sessionStorage
   ↓
7. Siguientes requests incluyen: Authorization: Bearer <token>
   ↓
8. JwtAuthenticationFilter valida token:
   - Firma JWT
   - No expirado
   - Establece SecurityContext
   ↓
9. SecurityConfig autoriza basado en rol
```

---

## 🔑 Roles y Permisos

| Rol | Descripción | Permisos |
|-----|-------------|----------|
| **ADMIN** | Administrador total | Acceso a todo, crear usuarios, iniciar búsquedas |
| **OPERADOR** | Operador de búsquedas | Iniciar búsquedas, ver resultados |
| **VIEWER** | Solo lectura | Solo ver resultados (sin iniciar búsquedas) |

---

## 🚦 Estados de Usuario

| Estado | Descripción |
|--------|-------------|
| **ACTIVO** | Puede acceder normalmente |
| **INACTIVO** | No puede acceder (requiere reactivación) |
| **BLOQUEADO** | Bloqueado por seguridad (requiere intervención admin) |

---

## 📝 Ejemplos en Frontend (JavaScript/TypeScript)

### Login
```javascript
const response = await fetch('http://localhost:8083/api/veritus-app/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    email: 'admin@veritus.com',
    password: 'admin123'
  })
});

const data = await response.json();
if (response.ok) {
  // Guardar token
  localStorage.setItem('jwtToken', data.token);
  localStorage.setItem('usuario', JSON.stringify(data.usuario));
  console.log('Login exitoso:', data.usuario);
} else {
  console.error('Error:', data.mensaje);
}
```

### Usar Token en Requests
```javascript
const token = localStorage.getItem('jwtToken');

const response = await fetch('http://localhost:8083/api/buscar-personas', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`  // ← Token aquí
  },
  body: JSON.stringify({ archivo: 'personas.csv' })
});
```

---

## 🛡️ SOLID Principles Implementados

✅ **Single Responsibility**
- Cada servicio tiene UNA responsabilidad
- AuthenticationService: solo autenticación
- JwtTokenProvider: solo gestión de JWTs

✅ **Open/Closed**
- Fácil agregar nuevos roles sin cambiar código
- Enums extensibles

✅ **Liskov Substitution**
- Interfaces bien definidas
- Repositorio sigue patrón JpaRepository

✅ **Interface Segregation**
- DTOs pequeños y específicos
- Interfaces mínimas

✅ **Dependency Inversion**
- Inyección de dependencias via @Autowired
- Servicios dependen de interfaces (repositorios)

---

## 🐛 Troubleshooting

### Token Expirado
**Error:** `TokenInvalidoException: Token inválido o expirado`
**Solución:** Generar nuevo token (hacer login nuevamente)

### Usuario Bloqueado
**Error:** `UsuarioBloqueadoException: Usuario bloqueado`
**Solución:** Contactar administrador para desbloquear

### CORS Error
**Error:** `Access to XMLHttpRequest... has been blocked by CORS policy`
**Solución:** Verificar `app.cors.allowed-origins` en `application.properties`

---

## 📚 Archivos Relacionados

- Controladores: `/src/main/java/com/example/veritusbot/controller/`
- Servicios: `/src/main/java/com/example/veritusbot/service/`
- Modelos: `/src/main/java/com/example/veritusbot/model/`
- Configuración: `/src/main/java/com/example/veritusbot/config/`
- Propiedades: `/src/main/resources/application.properties`

---

**Implementado:** 26 de Marzo, 2026
**Versión:** 1.0.0
**Estado:** ✅ Compilación exitosa

