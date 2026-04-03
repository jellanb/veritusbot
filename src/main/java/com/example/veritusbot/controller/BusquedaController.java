package com.example.veritusbot.controller;

import com.example.veritusbot.dto.BusyResponseDTO;
import com.example.veritusbot.dto.BusquedaPersonasAcceptedResponseDTO;
import com.example.veritusbot.dto.ErrorResponseDTO;
import com.example.veritusbot.dto.PersonaDTO;
import com.example.veritusbot.exception.InvalidClientFileException;
import com.example.veritusbot.service.AsyncProcessingService;
import com.example.veritusbot.service.ClientFileParserService;
import com.example.veritusbot.service.ProcessingStateManager;
import com.example.veritusbot.service.SearchRuntimeConfigService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
public class BusquedaController {

    private static final Logger logger = LoggerFactory.getLogger(BusquedaController.class);

    private final ClientFileParserService clientFileParserService;
    private final AsyncProcessingService asyncProcessingService;
    private final ProcessingStateManager processingStateManager;
    private final SearchRuntimeConfigService searchRuntimeConfigService;

    public BusquedaController(ClientFileParserService clientFileParserService,
                              AsyncProcessingService asyncProcessingService,
                              ProcessingStateManager processingStateManager,
                              SearchRuntimeConfigService searchRuntimeConfigService) {
        this.clientFileParserService = clientFileParserService;
        this.asyncProcessingService = asyncProcessingService;
        this.processingStateManager = processingStateManager;
        this.searchRuntimeConfigService = searchRuntimeConfigService;
    }

    /**
     * Endpoint to search for people asynchronously
     * POST /api/buscar-personas?isAllRegionEnabled=true&isSantiagoEnabled=true
     * Content-Type: multipart/form-data (campo: file)
     *
     * Returns:
     * - 202 Accepted if request accepted and processing started
     * - 429 Too Many Requests if system is busy
     */
    @PostMapping("/api/buscar-personas")
    public ResponseEntity<?> searchPeople(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "isAllRegionEnabled", defaultValue = "true") boolean isAllRegionEnabled,
            @RequestParam(value = "isSantiagoEnabled", defaultValue = "true") boolean isSantiagoEnabled,
            @RequestParam(value = "searchName", required = false) String searchName) {

        try {
            String requestId = UUID.randomUUID().toString();
            logger.info("Solicitud de busqueda recibida requestId={} allRegion={} santiago={}",
                    requestId,
                    isAllRegionEnabled,
                    isSantiagoEnabled);

            if (asyncProcessingService.isBusy()) {
                ProcessingStateManager.ProcessingState state = asyncProcessingService.getState();
                String message = String.format(
                    "El sistema esta ocupado procesando '%s'. Intenta nuevamente en unos minutos.",
                    state.getCurrentPerson() == null ? "N/A" : state.getCurrentPerson()
                );

                BusyResponseDTO response = new BusyResponseDTO();
                response.setStatus("BUSY");
                response.setMessage(message);
                response.setCurrentPerson(state.getCurrentPerson());
                response.setProcessingTimeMs(state.getProcessingTimeMs());

                logger.warn("requestId={} rechazada por sistema ocupado", requestId);

                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
            }

            List<PersonaDTO> people = clientFileParserService.parseAndValidate(file);

            int threadsPerPerson = searchRuntimeConfigService.getThreadsPerPerson();
            String effectiveSearchName = (searchName == null || searchName.isBlank())
                    ? "Busqueda " + requestId
                    : searchName.trim();

            asyncProcessingService.processSearchAsync(
                    people,
                    requestId,
                    effectiveSearchName,
                    isAllRegionEnabled,
                    isSantiagoEnabled,
                    threadsPerPerson
            );

            BusquedaPersonasAcceptedResponseDTO response = new BusquedaPersonasAcceptedResponseDTO();
            response.setStatus("ACCEPTED");
            response.setMessage("Solicitud aceptada. La busqueda fue iniciada en segundo plano");
            response.setRequestId(requestId);
            response.setPeopleCount(people.size());
            response.setAllRegionEnabled(isAllRegionEnabled);
            response.setSantiagoEnabled(isSantiagoEnabled);
            response.setThreadsPerPerson(threadsPerPerson);
            response.setProcessingMessage("El proceso de busqueda ya esta en ejecucion");
            response.setSearchName(effectiveSearchName);

            logger.info("requestId={} aceptada con {} clientes", requestId, people.size());

            return ResponseEntity.accepted().body(response);

        } catch (InvalidClientFileException e) {
            logger.warn("Error de validacion del archivo: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    new ErrorResponseDTO("Archivo invalido", "INVALID_FILE", e.getMessage())
            );
        } catch (Exception e) {
            logger.error("Error interno al procesar solicitud de busqueda", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ErrorResponseDTO("Error interno al procesar la solicitud", "INTERNAL_ERROR")
            );
        }
    }

    /**
     * Endpoint to get current processing status
     * GET /api/status
     */
    @GetMapping("/api/status")
    public ResponseEntity<?> getStatus() {
        ProcessingStateManager.ProcessingState state = processingStateManager.getState();

        Map<String, Object> response = new HashMap<>();
        response.put("isProcessing", state.isProcessing());
        response.put("currentPerson", state.getCurrentPerson());
        response.put("totalRequests", state.getTotalRequests());
        response.put("completedRequests", state.getCompletedRequests());
        response.put("processingTimeMs", state.getProcessingTimeMs());

        if (state.isProcessing()) {
            response.put("message", "System is busy processing: " + state.getCurrentPerson());
            response.put("status", "BUSY");
        } else {
            response.put("message", "System is idle");
            response.put("status", "IDLE");
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint para solicitar la detencion de una busqueda en curso.
     * POST /api/buscar-personas/detener
     */
    @PostMapping("/api/buscar-personas/detener")
    public ResponseEntity<?> stopSearch() {
        if (!asyncProcessingService.isBusy()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    new ErrorResponseDTO("No hay una busqueda en ejecucion", "NO_ACTIVE_SEARCH")
            );
        }

        asyncProcessingService.requestStop();

        Map<String, Object> response = new HashMap<>();
        response.put("status", "STOP_REQUESTED");
        response.put("message", "Se solicito detener la busqueda en curso");

        return ResponseEntity.accepted().body(response);
    }

    /**
     * Health check endpoint
     * GET /api/health
     */
    @GetMapping("/api/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("✓ API is working correctly");
    }

    /**
     * Legacy test endpoint
     * GET /api/test
     */
    @GetMapping("/api/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("✓ API is working correctly");
    }
}
