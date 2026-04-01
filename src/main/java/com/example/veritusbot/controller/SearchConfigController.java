package com.example.veritusbot.controller;

import com.example.veritusbot.dto.ErrorResponseDTO;
import com.example.veritusbot.dto.UpdateSearchThreadsRequestDTO;
import com.example.veritusbot.service.SearchRuntimeConfigService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/veritus-app/search-config")
public class SearchConfigController {

    private final SearchRuntimeConfigService searchRuntimeConfigService;

    public SearchConfigController(SearchRuntimeConfigService searchRuntimeConfigService) {
        this.searchRuntimeConfigService = searchRuntimeConfigService;
    }

    @GetMapping("/threads")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getThreadsConfig() {
        return ResponseEntity.ok(searchRuntimeConfigService.getCurrentConfig());
    }

    @PutMapping("/threads")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateThreadsConfig(@RequestBody UpdateSearchThreadsRequestDTO request) {
        try {
            if (request == null || !request.isValid()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponseDTO("threadsPerPerson es obligatorio y debe ser mayor a 0", "DATOS_INVALIDOS"));
            }

            int updatedValue = searchRuntimeConfigService.updateThreadsPerPerson(request.getThreadsPerPerson());

            Map<String, Object> response = new HashMap<>();
            response.put("status", "UPDATED");
            response.put("message", "Configuracion de hilos actualizada en memoria");
            response.put("threadsPerPerson", updatedValue);
            response.putAll(searchRuntimeConfigService.getCurrentConfig());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponseDTO(e.getMessage(), "DATOS_INVALIDOS"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDTO("Error interno del servidor", "ERROR_INTERNO", e.getMessage()));
        }
    }
}

