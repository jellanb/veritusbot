package com.example.veritusbot.controller;

import com.example.veritusbot.dto.CausaReportResponseDTO;
import com.example.veritusbot.dto.ErrorResponseDTO;
import com.example.veritusbot.service.CausaReportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador para exponer el reporte de causas con filtros.
 */
@RestController
@RequestMapping("/api/veritus-app/causas")
public class CausaReportController {

    private final CausaReportService causaReportService;

    public CausaReportController(CausaReportService causaReportService) {
        this.causaReportService = causaReportService;
    }

    @GetMapping("/reporte")
    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR','VIEWER')")
    public ResponseEntity<?> getReporte(@RequestParam(required = false) Integer anioDesde,
                                        @RequestParam(required = false) Integer anioHasta,
                                        @RequestParam(required = false) String tribunal,
                                        @RequestParam(defaultValue = "desc") String sortDir,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "50") int size) {
        try {
            CausaReportResponseDTO response = causaReportService
                    .getReporte(anioDesde, anioHasta, tribunal, sortDir, page, size);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponseDTO(e.getMessage(), "PARAMETROS_INVALIDOS"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDTO("Error al generar el reporte", "ERROR_INTERNO", e.getMessage()));
        }
    }
}

