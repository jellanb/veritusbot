package com.example.veritusbot.controller;

import com.example.veritusbot.dto.ErrorResponseDTO;
import com.example.veritusbot.dto.LoginRequestDTO;
import com.example.veritusbot.dto.LoginResponseDTO;
import com.example.veritusbot.exception.ContrasenaInvalidaException;
import com.example.veritusbot.exception.UsuarioBloqueadoException;
import com.example.veritusbot.exception.UsuarioNoEncontradoException;
import com.example.veritusbot.service.auth.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Controlador para autenticación de usuarios.
 * Expone el endpoint de login para que el frontend autentique usuarios.
 *
 * SOLID Principle - Single Responsibility:
 * Solo es responsable de recibir requests HTTP y delegar a servicios.
 */
@RestController
@RequestMapping("/api/veritus-app")
public class LoginController {

    @Autowired
    private AuthenticationService authenticationService;

    /**
     * Endpoint para autenticar usuarios.
     * Recibe credenciales y retorna un token JWT si son válidas.
     *
     * REQUEST:
     * POST /api/veritus-app/login
     * {
     *   "email": "usuario@example.com",
     *   "password": "contraseña123"
     * }
     *
     * RESPONSE (200 OK):
     * {
     *   "token": "eyJhbGciOiJIUzUxMiJ9...",
     *   "usuario": {
     *     "id": "uuid",
     *     "email": "usuario@example.com",
     *     "nombreCompleto": "John Doe",
     *     "rol": "ADMIN",
     *     "estado": "ACTIVO"
     *   },
     *   "loginAt": "2026-03-26T10:30:00"
     * }
     *
     * @param loginRequest DTO con credenciales del usuario
     * @return ResponseEntity con token y datos del usuario o error
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO loginRequest) {
        try {
            System.out.println("\n╔════════════════════════════════════════════════════════════╗");
            System.out.println("║  LOGIN REQUEST RECEIVED                                    ║");
            System.out.println("║  Email: " + loginRequest.getEmail());
            System.out.println("╚════════════════════════════════════════════════════════════╝\n");

            // Delegar al servicio de autenticación
            LoginResponseDTO response = authenticationService.login(loginRequest);

            System.out.println("✅ Login exitoso para: " + loginRequest.getEmail());
            return ResponseEntity.ok(response);

        } catch (UsuarioNoEncontradoException e) {
            // Usuario no existe o credenciales inválidas
            System.out.println("❌ Login fallido - Usuario no encontrado: " + loginRequest.getEmail());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponseDTO(
                            e.getMessage(),
                            "USUARIO_NO_ENCONTRADO"
                    ));

        } catch (ContrasenaInvalidaException e) {
            // Contraseña incorrecta
            System.out.println("❌ Login fallido - Contraseña inválida: " + loginRequest.getEmail());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponseDTO(
                            e.getMessage(),
                            "CONTRASENA_INVALIDA"
                    ));

        } catch (UsuarioBloqueadoException e) {
            // Usuario bloqueado
            System.out.println("❌ Login fallido - Usuario bloqueado: " + loginRequest.getEmail());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponseDTO(
                            e.getMessage(),
                            "USUARIO_BLOQUEADO"
                    ));

        } catch (IllegalArgumentException e) {
            // Validación de datos fallida
            System.out.println("❌ Login fallido - Datos inválidos");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponseDTO(
                            e.getMessage(),
                            "DATOS_INVALIDOS"
                    ));

        } catch (Exception e) {
            // Error interno del servidor
            System.out.println("❌ Error interno en login: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDTO(
                            "Error interno del servidor",
                            "ERROR_INTERNO",
                            e.getMessage()
                    ));
        }
    }

    /**
     * Endpoint para verificar si un usuario está autenticado.
     * Devuelve información básica del usuario autenticado.
     *
     * REQUEST:
     * GET /api/veritus-app/me
     * Headers: Authorization: Bearer <JWT_TOKEN>
     *
     * RESPONSE (200 OK):
     * {
     *   "id": "uuid",
     *   "email": "usuario@example.com",
     *   "rol": "ADMIN",
     *   "estado": "ACTIVO"
     * }
     *
     * @return ResponseEntity con datos del usuario actual
     */
    @PostMapping("/health-auth")
    public ResponseEntity<?> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now());
        response.put("endpoint", "/api/veritus-app/login");
        response.put("metodo", "POST");
        return ResponseEntity.ok(response);
    }
}

