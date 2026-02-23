package com.example.veritusbot.controller;

import com.example.veritusbot.scraper.PjudScraper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BusquedaController {

    @Autowired
    private PjudScraper pjudScraper;

    /**
     * Endpoint para buscar personas desde un archivo CSV
     * GET /api/buscar-personas?archivo=personas.csv
     */
    @GetMapping("/api/buscar-personas")
    public ResponseEntity<String> buscarPersonas(
            @RequestParam(value = "archivo", defaultValue = "personas.csv") String archivo) {

        try {
            System.out.println("\n╔════════════════════════════════════════════════════════════╗");
            System.out.println("║  INICIANDO BÚSQUEDA DESDE API                             ║");
            System.out.println("║  Archivo: " + archivo);
            System.out.println("╚════════════════════════════════════════════════════════════╝\n");

            pjudScraper.buscarPersonasDelExcel(archivo);

            return ResponseEntity.ok("✓ Búsqueda completada. Revisa los logs.");

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("❌ Error: " + e.getMessage());
        }
    }

    /**
     * Endpoint de prueba
     */
    @GetMapping("/api/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("✓ API funcionando correctamente");
    }
}
