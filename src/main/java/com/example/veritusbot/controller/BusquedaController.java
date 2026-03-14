package com.example.veritusbot.controller;

import com.example.veritusbot.dto.PersonaDTO;
import com.example.veritusbot.dto.ResultDTO;
import com.example.veritusbot.service.ExcelService;
import com.example.veritusbot.service.scraper.ScraperOrchestrator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
public class BusquedaController {

    @Autowired
    private ScraperOrchestrator scraperOrchestrator;

    @Autowired
    private ExcelService excelService;

    /**
     * Endpoint to search for people from CSV file
     * POST /api/buscar-personas?archivo=personas.csv
     */
    @PostMapping("/api/buscar-personas")
    public ResponseEntity<String> searchPeople(
            @RequestParam(value = "archivo", defaultValue = "personas.csv") String archivo) {

        try {
            System.out.println("\n╔════════════════════════════════════════════════════════════╗");
            System.out.println("║  STARTING SEARCH FROM API                                 ║");
            System.out.println("║  File: " + archivo);
            System.out.println("╚════════════════════════════════════════════════════════════╝\n");

            // Read people from CSV
            List<PersonaDTO> people = excelService.readClientFromCSV(archivo);

            if (people.isEmpty()) {
                return ResponseEntity.ok("⚠ No people found in CSV file");
            }

            System.out.println("📊 Loaded " + people.size() + " people from " + archivo);

            // Execute scraping using new orchestrator
            List<ResultDTO> results = scraperOrchestrator.scrapePeople(people);

            System.out.println("\n✅ Search completed. Total results: " + results.size());
            System.out.println("📋 Results saved to: resultados_busqueda.csv\n");

            return ResponseEntity.ok("✓ Search completed. Found " + results.size() + " results. Check logs for details.");

        } catch (Exception e) {
            System.err.println("❌ Error during search: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body("❌ Error: " + e.getMessage());
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/api/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("✓ API is working correctly");
    }

    /**
     * Get status of the scraper
     */
    @GetMapping("/api/status")
    public ResponseEntity<String> status() {
        return ResponseEntity.ok("✓ Scraper service is operational");
    }
}
