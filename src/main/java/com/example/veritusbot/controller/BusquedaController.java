package com.example.veritusbot.controller;

import com.example.veritusbot.dto.PersonaDTO;
import com.example.veritusbot.service.AsyncProcessingService;
import com.example.veritusbot.service.ExcelService;
import com.example.veritusbot.service.ProcessingStateManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class BusquedaController {

    @Autowired
    private ExcelService excelService;

    @Autowired
    private AsyncProcessingService asyncProcessingService;

    @Autowired
    private ProcessingStateManager processingStateManager;

    /**
     * Endpoint to search for people asynchronously
     * POST /api/buscar-personas?archivo=personas.csv&isAllRegionEnabled=true
     * 
     * Returns:
     * - 202 Accepted if request accepted and processing started
     * - 429 Too Many Requests if system is busy
     */
    @PostMapping("/api/buscar-personas")
    public ResponseEntity<?> searchPeople(
            @RequestParam(value = "archivo", defaultValue = "personas.csv") String archivo,
            @RequestParam(value = "isAllRegionEnabled", defaultValue = "true") boolean isAllRegionEnabled) {

        try {
            // Log request
            String requestId = UUID.randomUUID().toString();
            System.out.println("\n╔════════════════════════════════════════════════════════════╗");
            System.out.println("║  SEARCH REQUEST RECEIVED                                     ║");
            System.out.println("║  Request ID: " + requestId);
            System.out.println("║  File: " + archivo);
            System.out.println("║  All Region Enabled: " + isAllRegionEnabled);
            System.out.println("╚════════════════════════════════════════════════════════════╝\n");

            // Check if system is busy
            if (asyncProcessingService.isBusy()) {
                ProcessingStateManager.ProcessingState state = asyncProcessingService.getState();
                String message = String.format(
                    "System is busy processing '%s'. Please try again later.",
                    state.getCurrentPerson()
                );

                System.out.println("⚠️  " + message);
                System.out.println("📊 Processing: " + state.getTotalRequests() + " total requests");

                Map<String, Object> response = new HashMap<>();
                response.put("status", "BUSY");
                response.put("message", message);
                response.put("currentPerson", state.getCurrentPerson());
                response.put("processingTimeMs", state.getProcessingTimeMs());

                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
            }

            // Read people from CSV
            List<PersonaDTO> people = excelService.readClientFromCSV(archivo);

            if (people.isEmpty()) {
                return ResponseEntity.ok("⚠ No people found in CSV file");
            }

            System.out.println("📊 Loaded " + people.size() + " people from " + archivo);

            // Launch async processing
            asyncProcessingService.processSearchAsync(people, requestId, isAllRegionEnabled);

            // Return 202 Accepted immediately
            Map<String, Object> response = new HashMap<>();
            response.put("status", "ACCEPTED");
            response.put("message", "Request accepted and processing started");
            response.put("requestId", requestId);
            response.put("peopleCount", people.size());
            response.put("isAllRegionEnabled", isAllRegionEnabled);
            response.put("processingMessage", "Search is being processed in the background");

            System.out.println("✅ Request accepted: " + requestId);
            System.out.println("🔄 Processing started in background");
            System.out.println("📋 Results will be saved to: resultados_busqueda.csv\n");

            return ResponseEntity.accepted().body(response);

        } catch (Exception e) {
            System.err.println("❌ Error processing request: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "ERROR");
            errorResponse.put("message", "Error: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
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
