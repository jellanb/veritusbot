package com.example.veritusbot.controller;

import com.example.veritusbot.dto.CreateProxiSettingRequestDTO;
import com.example.veritusbot.dto.ErrorResponseDTO;
import com.example.veritusbot.dto.ProxiSettingDTO;
import com.example.veritusbot.service.ProxiSettingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador para gestión de proxies del sistema.
 *
 * Endpoints disponibles (todos requieren rol ADMIN):
 *   GET    /api/veritus-app/proxies         → Listar todos los proxies
 *   POST   /api/veritus-app/proxies         → Agregar proxy
 *   PUT    /api/veritus-app/proxies/{id}    → Actualizar proxy
 *   DELETE /api/veritus-app/proxies/{id}    → Eliminar proxy
 *
 * SOLID Principle - Single Responsibility:
 * Solo maneja HTTP y delega lógica a ProxiSettingService.
 */
@RestController
@RequestMapping("/api/veritus-app/proxies")
public class ProxiSettingController {

    private final ProxiSettingService proxiSettingService;

    public ProxiSettingController(ProxiSettingService proxiSettingService) {
        this.proxiSettingService = proxiSettingService;
    }

    // ==================== LISTAR PROXIES ====================

    /**
     * Retorna la lista de todos los proxies configurados en el sistema.
     *
     * REQUEST:
     * GET /api/veritus-app/proxies
     * Authorization: Bearer <JWT_TOKEN_ADMIN>
     *
     * RESPONSE (200 OK):
     * [
     *   { "id": 1, "server": "http://proxy1:8080", "username": "user1", "password": "***", "activo": true, "orden": 1 },
     *   ...
     * ]
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> listar() {
        try {
            List<ProxiSettingDTO> proxies = proxiSettingService.listarTodos();
            return ResponseEntity.ok(proxies);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDTO("Error al obtener proxies", "ERROR_INTERNO", e.getMessage()));
        }
    }

    // ==================== AGREGAR PROXY ====================

    /**
     * Agrega un nuevo proxy al sistema.
     * Solo ADMIN puede agregar proxies.
     *
     * REQUEST:
     * POST /api/veritus-app/proxies
     * Authorization: Bearer <JWT_TOKEN_ADMIN>
     * Content-Type: application/json
     * {
     *   "server":   "http://proxy.example.com:8080",   ← OBLIGATORIO
     *   "username": "mi_usuario",                       ← opcional
     *   "password": "mi_clave",                         ← opcional
     *   "activo":   true,                               ← opcional, default: true
     *   "orden":    1                                   ← opcional, default: 0
     * }
     *
     * RESPONSE (201 Created):
     * { "id": 1, "server": "http://proxy.example.com:8080", "username": "mi_usuario", "password": "mi_clave", "activo": true, "orden": 1 }
     *
     * RESPONSE (400 Bad Request) → server vacío o nulo:
     * { "mensaje": "El campo 'server' es obligatorio...", "codigo": "DATOS_INVALIDOS" }
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> agregar(@RequestBody CreateProxiSettingRequestDTO request) {
        try {
            System.out.println("\n╔══════════════════════════════════════════════════════╗");
            System.out.println("║  AGREGAR PROXY REQUEST                               ║");
            System.out.println("║  Server: " + request.getServer());
            System.out.println("║  Auth:   " + (request.getUsername() != null && !request.getUsername().isBlank()));
            System.out.println("║  Orden:  " + request.getOrden());
            System.out.println("╚══════════════════════════════════════════════════════╝\n");

            ProxiSettingDTO creado = proxiSettingService.crear(request);

            System.out.println("✅ Proxy agregado exitosamente: " + creado.getServer() + " (id=" + creado.getId() + ")");
            return ResponseEntity.status(HttpStatus.CREATED).body(creado);

        } catch (IllegalArgumentException e) {
            System.out.println("❌ Datos inválidos al agregar proxy: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponseDTO(e.getMessage(), "DATOS_INVALIDOS"));

        } catch (Exception e) {
            System.out.println("❌ Error interno al agregar proxy: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDTO("Error interno del servidor", "ERROR_INTERNO", e.getMessage()));
        }
    }

    // ==================== ACTUALIZAR PROXY ====================

    /**
     * Actualiza un proxy existente por su ID.
     *
     * REQUEST:
     * PUT /api/veritus-app/proxies/{id}
     * Authorization: Bearer <JWT_TOKEN_ADMIN>
     * Content-Type: application/json
     * {
     *   "server":   "http://nuevo-proxy:8080",
     *   "username": "usuario",
     *   "password": "clave",
     *   "activo":   false,
     *   "orden":    2
     * }
     *
     * RESPONSE (200 OK): ProxiSettingDTO actualizado
     * RESPONSE (404 Not Found): proxy no existe
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody ProxiSettingDTO dto) {
        try {
            System.out.println("🔄 Actualizar proxy id=" + id + " → " + dto.getServer());
            ProxiSettingDTO actualizado = proxiSettingService.actualizar(id, dto);
            System.out.println("✅ Proxy actualizado: id=" + id);
            return ResponseEntity.ok(actualizado);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponseDTO(e.getMessage(), "PROXY_NO_ENCONTRADO"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDTO("Error interno del servidor", "ERROR_INTERNO", e.getMessage()));
        }
    }

    // ==================== ELIMINAR PROXY ====================

    /**
     * Elimina un proxy por su ID.
     *
     * REQUEST:
     * DELETE /api/veritus-app/proxies/{id}
     * Authorization: Bearer <JWT_TOKEN_ADMIN>
     *
     * RESPONSE (204 No Content): eliminado correctamente
     * RESPONSE (404 Not Found): proxy no existe
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try {
            System.out.println("🗑️ Eliminar proxy id=" + id);
            proxiSettingService.eliminar(id);
            System.out.println("✅ Proxy eliminado: id=" + id);
            return ResponseEntity.noContent().build();

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponseDTO(e.getMessage(), "PROXY_NO_ENCONTRADO"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDTO("Error interno del servidor", "ERROR_INTERNO", e.getMessage()));
        }
    }
}
