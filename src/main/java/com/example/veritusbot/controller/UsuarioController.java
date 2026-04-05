package com.example.veritusbot.controller;

import com.example.veritusbot.dto.CreateUsuarioRequestDTO;
import com.example.veritusbot.dto.ErrorResponseDTO;
import com.example.veritusbot.dto.UpdateUsuarioRequestDTO;
import com.example.veritusbot.dto.UsuarioDTO;
import com.example.veritusbot.exception.UsuarioNoEncontradoException;
import com.example.veritusbot.exception.UsuarioYaExisteException;
import com.example.veritusbot.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controlador para gestión de usuarios del sistema.
 *
 * Endpoints disponibles:
 *   POST   /api/veritus-app/usuarios           → Crear usuario (solo ADMIN)
 *   GET    /api/veritus-app/usuarios            → Listar usuarios (solo ADMIN)
 *   GET    /api/veritus-app/usuarios/{id}       → Buscar usuario por ID (solo ADMIN)
 *
 * SOLID Principle - Single Responsibility:
 * Solo maneja HTTP y delega lógica a UsuarioService.
 */
@RestController
@RequestMapping("/api/veritus-app/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    // ==================== CREAR USUARIO ====================

    /**
     * Crea un nuevo usuario en el sistema.
     * Solo usuarios con rol ADMIN pueden crear nuevos usuarios.
     *
     * REQUEST:
     * POST /api/veritus-app/usuarios
     * Authorization: Bearer <JWT_TOKEN_ADMIN>
     * {
     *   "email": "nuevo@example.com",
     *   "password": "contraseña123",
     *   "nombreCompleto": "Juan Pérez",
     *   "rol": "OPERADOR"        ← opcional, default: VIEWER
     * }
     *
     * RESPONSE (201 Created):
     * {
     *   "id": "uuid",
     *   "email": "nuevo@example.com",
     *   "nombreCompleto": "Juan Pérez",
     *   "rol": "OPERADOR",
     *   "estado": "ACTIVO",
     *   "createdAt": "2026-03-26T10:30:00"
     * }
     *
     * @param request DTO con datos del nuevo usuario
     * @return UsuarioDTO del usuario creado o error
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> crearUsuario(@RequestBody CreateUsuarioRequestDTO request) {
        try {
            System.out.println("\n╔══════════════════════════════════════════════════════╗");
            System.out.println("║  CREAR USUARIO REQUEST                               ║");
            System.out.println("║  Email: " + request.getEmail());
            System.out.println("║  Rol:   " + request.getRol());
            System.out.println("╚══════════════════════════════════════════════════════╝\n");

            UsuarioDTO usuarioCreado = usuarioService.crearUsuario(request);

            System.out.println("✅ Usuario creado exitosamente: " + usuarioCreado.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED).body(usuarioCreado);

        } catch (UsuarioYaExisteException e) {
            System.out.println("❌ Email ya registrado: " + request.getEmail());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponseDTO(e.getMessage(), "EMAIL_YA_REGISTRADO"));

        } catch (IllegalArgumentException e) {
            System.out.println("❌ Datos inválidos al crear usuario: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponseDTO(e.getMessage(), "DATOS_INVALIDOS"));

        } catch (Exception e) {
            System.out.println("❌ Error interno al crear usuario: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDTO("Error interno del servidor", "ERROR_INTERNO", e.getMessage()));
        }
    }

    // ==================== LISTAR USUARIOS ====================

    /**
     * Retorna la lista de todos los usuarios del sistema.
     * Solo ADMIN puede listar usuarios.
     *
     * REQUEST:
     * GET /api/veritus-app/usuarios
     * Authorization: Bearer <JWT_TOKEN_ADMIN>
     *
     * RESPONSE (200 OK):
     * [
     *   { "id": "...", "email": "...", "rol": "ADMIN", "estado": "ACTIVO" },
     *   ...
     * ]
     *
     * @return lista de UsuarioDTO
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> listarUsuarios() {
        try {
            List<UsuarioDTO> usuarios = usuarioService.listarUsuarios();
            return ResponseEntity.ok(usuarios);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDTO("Error al obtener usuarios", "ERROR_INTERNO", e.getMessage()));
        }
    }

    // ==================== BUSCAR POR ID ====================

    /**
     * Busca un usuario por su UUID.
     * Solo ADMIN puede consultar usuarios.
     *
     * REQUEST:
     * GET /api/veritus-app/usuarios/{id}
     * Authorization: Bearer <JWT_TOKEN_ADMIN>
     *
     * RESPONSE (200 OK):
     * { "id": "...", "email": "...", "rol": "ADMIN", "estado": "ACTIVO" }
     *
     * @param id UUID del usuario
     * @return UsuarioDTO si existe
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> buscarPorId(@PathVariable UUID id) {
        try {
            UsuarioDTO usuario = usuarioService.buscarPorId(id);
            return ResponseEntity.ok(usuario);

        } catch (UsuarioNoEncontradoException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponseDTO(e.getMessage(), "USUARIO_NO_ENCONTRADO"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDTO("Error interno del servidor", "ERROR_INTERNO", e.getMessage()));
        }
    }

    // ==================== EDITAR USUARIO ====================

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> editarUsuario(@PathVariable UUID id, @RequestBody UpdateUsuarioRequestDTO request) {
        try {
            UsuarioDTO usuarioActualizado = usuarioService.editarUsuario(id, request);
            return ResponseEntity.ok(usuarioActualizado);

        } catch (UsuarioNoEncontradoException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponseDTO(e.getMessage(), "USUARIO_NO_ENCONTRADO"));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponseDTO(e.getMessage(), "DATOS_INVALIDOS"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDTO("Error interno del servidor", "ERROR_INTERNO", e.getMessage()));
        }
    }

    // ==================== DESACTIVAR USUARIO ====================

    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> desactivarUsuario(@PathVariable UUID id) {
        try {
            UsuarioDTO usuarioDesactivado = usuarioService.desactivarUsuario(id);
            return ResponseEntity.ok(usuarioDesactivado);

        } catch (UsuarioNoEncontradoException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponseDTO(e.getMessage(), "USUARIO_NO_ENCONTRADO"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDTO("Error interno del servidor", "ERROR_INTERNO", e.getMessage()));
        }
    }

    // ==================== ELIMINAR USUARIO ====================

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> eliminarUsuario(@PathVariable UUID id) {
        try {
            usuarioService.eliminarUsuario(id);
            return ResponseEntity.noContent().build();

        } catch (UsuarioNoEncontradoException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponseDTO(e.getMessage(), "USUARIO_NO_ENCONTRADO"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDTO("Error interno del servidor", "ERROR_INTERNO", e.getMessage()));
        }
    }
}

