package com.example.veritusbot.service;

import com.example.veritusbot.model.TribunalBusqueda;
import com.example.veritusbot.repository.TribunalBusquedaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Service to track tribunal searches during scraping
 * Validates that all tribunals completed successfully before marking person as processed
 */
@Service
public class TribunalBusquedaService {
    private static final Logger logger = LoggerFactory.getLogger(TribunalBusquedaService.class);

    private final TribunalBusquedaRepository tribunalBusquedaRepository;

    public TribunalBusquedaService(TribunalBusquedaRepository tribunalBusquedaRepository) {
        this.tribunalBusquedaRepository = tribunalBusquedaRepository;
    }

    /**
     * Register all tribunals that will be searched for a person
     * Called BEFORE starting the search for each year/phase
     *
     * @param personaProcessadaId ID of person being searched
     * @param solicitudId Request ID for tracking
     * @param anno Year to search
     * @param fase Phase (PHASE_1 or PHASE_2)
     * @param nombresTribunales List of tribunal names to search
     * @param tribunalIndexMap Map of tribunal name to index
     */
    public void registrarTribunalesAProcesar(
            Integer personaProcessadaId,
            String solicitudId,
            Integer anno,
            String fase,
            List<String> nombresTribunales,
            Map<String, Integer> tribunalIndexMap) {

        logger.info("📋 Registering {} tribunals for person {} [{}] anno={}, fase={}",
                nombresTribunales.size(), personaProcessadaId, solicitudId, anno, fase);

        for (String tribunal : nombresTribunales) {
            TribunalBusqueda tb = tribunalBusquedaRepository
                    .findByPersonaProcessadaIdAndSolicitudIdAndTribunalNombreAndAnnoAndFase(
                            personaProcessadaId,
                            solicitudId,
                            tribunal,
                            anno,
                            fase)
                    .orElseGet(() -> new TribunalBusqueda(
                            personaProcessadaId,
                            solicitudId,
                            anno,
                            fase,
                            tribunal,
                            tribunalIndexMap.getOrDefault(tribunal, -1)
                    ));

            if (tb.getId() == null) {
                tribunalBusquedaRepository.save(tb);
                logger.debug("✓ Registered tribunal: {}", tribunal);
            }
        }

        logger.info("✅ Registered {} tribunals to process", nombresTribunales.size());
    }

    /**
     * Mark tribunal search as completed
     * Called AFTER each tribunal search (successful or with errors)
     *
     * @param personaProcessadaId ID of person
     * @param tribunal Tribunal name
     * @param fase Phase
     * @param anno Year
     * @param exitoso true if search completed without scraper errors
     * @param motivoError Error message if applicable
     */
    public void marcarTribunalCompletado(
            Integer personaProcessadaId,
            String solicitudId,
            String tribunal,
            String fase,
            Integer anno,
            boolean exitoso,
            String motivoError) {

        TribunalBusqueda tb = tribunalBusquedaRepository
                .findByPersonaProcessadaIdAndSolicitudIdAndTribunalNombreAndAnnoAndFase(
                        personaProcessadaId,
                        solicitudId,
                        tribunal,
                        anno,
                        fase)
                .orElse(null);

        if (tb == null) {
            logger.warn("⚠️ TribunalBusqueda not found for person {} request {} tribunal {} fase {}",
                    personaProcessadaId, solicitudId, tribunal, fase);
            return;
        }

        if (exitoso) {
            tb.setEstado("COMPLETADA");
            logger.debug("✅ Tribunal COMPLETADA: {}", tribunal);
        } else {
            // Determine error type
            if (motivoError != null && motivoError.toLowerCase().contains("scraper")) {
                tb.setEstado("ERROR_SCRAPER");
            } else {
                tb.setEstado("ERROR_CONEXION");
            }
            tb.setMotivoError(motivoError);
            logger.warn("❌ Tribunal {} error: {} -> {}", tribunal, tb.getEstado(), motivoError);
        }

        tb.setProcesadaEn(LocalDateTime.now());
        tribunalBusquedaRepository.save(tb);
    }

    /**
     * Check if person can be marked as processed
     * ONLY returns true if ALL tribunals completed successfully (COMPLETADA status)
     * Returns false if ANY tribunal has ERROR_SCRAPER or ERROR_CONEXION
     *
     * @param personaProcessadaId ID of person
     * @param anno Year
     * @param fase Phase
     * @return true only if all tribunals are COMPLETADA, false otherwise
     */
    public boolean puedeMarcarComoProcessada(Integer personaProcessadaId, String solicitudId, String fase) {
        List<TribunalBusqueda> tribunales = tribunalBusquedaRepository
                .findByPersonaProcessadaIdAndSolicitudIdAndFase(personaProcessadaId, solicitudId, fase);

        // If NO tribunals registered: cannot mark
        if (tribunales.isEmpty()) {
            logger.warn("⚠️ No tribunals found for person {} request {} fase {}", personaProcessadaId, solicitudId, fase);
            return false;
        }

        // Check for any errors
        boolean hayErrores = tribunales.stream()
                .anyMatch(t -> "ERROR_SCRAPER".equals(t.getEstado()) || "ERROR_CONEXION".equals(t.getEstado()));

        if (hayErrores) {
            List<TribunalBusqueda> errores = tribunalBusquedaRepository
                    .findErroredTribunals(personaProcessadaId, solicitudId, fase);
            logger.warn("⚠️ Found {} tribunals with errors, cannot mark as processed", errores.size());
            for (TribunalBusqueda error : errores) {
                logger.warn("   - {} [{}]: {}", error.getTribunalNombre(), error.getEstado(), error.getMotivoError());
            }
            return false;
        }

        // Check all completed
        boolean todosCOMPLETADO = tribunales.stream()
                .allMatch(t -> "COMPLETADA".equals(t.getEstado()));

        long completadas = tribunales.stream()
                .filter(t -> "COMPLETADA".equals(t.getEstado()))
                .count();

        if (todosCOMPLETADO) {
            logger.info("✅ All {} tribunals COMPLETADA - person can be marked as processed", tribunales.size());
            return true;
        } else {
            logger.warn("⚠️ Not all tribunals completed: {}/{} COMPLETADA", completadas, tribunales.size());
            return false;
        }
    }

    /**
     * Get statistics for a person's search
     */
    public Map<String, Object> getEstadisticas(Integer personaProcessadaId, String solicitudId, Integer anno, String fase) {
        List<TribunalBusqueda> tribunales = tribunalBusquedaRepository
                .findByPersonaProcessadaIdAndSolicitudIdAndAnnoAndFase(personaProcessadaId, solicitudId, anno, fase);

        long completadas = tribunales.stream().filter(t -> "COMPLETADA".equals(t.getEstado())).count();
        long erroresScraper = tribunales.stream().filter(t -> "ERROR_SCRAPER".equals(t.getEstado())).count();
        long erroresConexion = tribunales.stream().filter(t -> "ERROR_CONEXION".equals(t.getEstado())).count();
        long pendientes = tribunales.stream().filter(t -> "PENDIENTE".equals(t.getEstado())).count();

        return Map.of(
                "total", (long) tribunales.size(),
                "completadas", completadas,
                "errores_scraper", erroresScraper,
                "errores_conexion", erroresConexion,
                "pendientes", pendientes,
                "porcentaje_exito", tribunales.isEmpty() ? 0.0 : (completadas * 100.0 / tribunales.size())
        );
    }
}

