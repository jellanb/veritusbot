package com.example.veritusbot.controller;

import com.example.veritusbot.dto.ErrorResponseDTO;
import com.example.veritusbot.dto.UpdateWorkingHoursRequestDTO;
import com.example.veritusbot.util.WorkingHoursManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/veritus-app/working-hours")
public class WorkingHoursConfigController {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final WorkingHoursManager workingHoursManager;

    public WorkingHoursConfigController(WorkingHoursManager workingHoursManager) {
        this.workingHoursManager = workingHoursManager;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getConfig() {
        Map<String, Object> response = new HashMap<>();
        response.put("enabled", workingHoursManager.isWorkingHoursEnabled());
        response.put("horaInicio", workingHoursManager.getHoraInicio().format(TIME_FORMATTER));
        response.put("horaFin", workingHoursManager.getHoraFin().format(TIME_FORMATTER));
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

            if (request.getHoraInicio() != null || request.getHoraFin() != null) {
                if (request.getHoraInicio() == null || request.getHoraFin() == null) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(new ErrorResponseDTO("horaInicio y horaFin deben enviarse juntas", "DATOS_INVALIDOS"));
                }

                LocalTime horaInicio = parseTime(request.getHoraInicio(), "horaInicio");
                LocalTime horaFin = parseTime(request.getHoraFin(), "horaFin");
                workingHoursManager.setRangoHorario(horaInicio, horaFin);
            }

            workingHoursManager.setWorkingHoursEnabled(request.getEnabled());

            Map<String, Object> response = new HashMap<>();
            response.put("status", "UPDATED");
            response.put("enabled", workingHoursManager.isWorkingHoursEnabled());
            response.put("horaInicio", workingHoursManager.getHoraInicio().format(TIME_FORMATTER));
            response.put("horaFin", workingHoursManager.getHoraFin().format(TIME_FORMATTER));
            response.put("descripcion", workingHoursManager.getConfiguracionDescripcion());

            return ResponseEntity.ok(response);
        } catch (DateTimeParseException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponseDTO(e.getMessage(), "DATOS_INVALIDOS"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponseDTO(e.getMessage(), "DATOS_INVALIDOS"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDTO("Error interno del servidor", "ERROR_INTERNO", e.getMessage()));
        }
    }

    private LocalTime parseTime(String value, String fieldName) {
        try {
            return LocalTime.parse(value, TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new DateTimeParseException(fieldName + " debe tener formato HH:mm", value, e.getErrorIndex());
        }
    }
}

