package com.example.veritusbot.controller;

import com.example.veritusbot.dto.ErrorResponseDTO;
import com.example.veritusbot.dto.UpdateWorkingHoursRequestDTO;
import com.example.veritusbot.util.WorkingHoursManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/veritus-app/working-hours")
public class WorkingHoursConfigController {

    private final WorkingHoursManager workingHoursManager;

    public WorkingHoursConfigController(WorkingHoursManager workingHoursManager) {
        this.workingHoursManager = workingHoursManager;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getConfig() {
        Map<String, Object> response = new HashMap<>();
        response.put("enabled", workingHoursManager.isWorkingHoursEnabled());
        response.put("horaInicio", workingHoursManager.getHoraInicio().toString());
        response.put("horaFin", workingHoursManager.getHoraFin().toString());
        response.put("descripcion", workingHoursManager.getConfiguracionDescripcion());
        return ResponseEntity.ok(response);
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateConfig(@RequestBody UpdateWorkingHoursRequestDTO request) {
        try {
            if (request == null || !request.isValid()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponseDTO("enabled es obligatorio", "DATOS_INVALIDOS"));
            }

            workingHoursManager.setWorkingHoursEnabled(request.getEnabled());

            Map<String, Object> response = new HashMap<>();
            response.put("status", "UPDATED");
            response.put("enabled", workingHoursManager.isWorkingHoursEnabled());
            response.put("descripcion", workingHoursManager.getConfiguracionDescripcion());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDTO("Error interno del servidor", "ERROR_INTERNO", e.getMessage()));
        }
    }
}

