package com.example.veritusbot.service;

import com.example.veritusbot.dto.PersonaDTO;
import com.example.veritusbot.model.PersonaProcesada;
import com.example.veritusbot.repository.PersonaProcesadaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service responsible for managing PersonaProcesada persistence
 * Implements Single Responsibility Principle: only handles PersonaProcesada persistence
 * 
 * Tracks which persons have been processed and their processing status
 */
@Service
public class PersonaProcesadaPersistenceService {
    private static final Logger logger = LoggerFactory.getLogger(PersonaProcesadaPersistenceService.class);

    private final PersonaProcesadaRepository personaProcesadaRepository;

    public PersonaProcesadaPersistenceService(PersonaProcesadaRepository personaProcesadaRepository) {
        this.personaProcesadaRepository = personaProcesadaRepository;
    }

    /**
     * Save a new PersonaProcesada with initial status (no processing yet)
     * Called when a new person is loaded from CSV
     * 
     * @param personDTO The person data to track
     * @return The saved PersonaProcesada
     */
    public PersonaProcesada saveNewPersonaProcesada(PersonaDTO personDTO) {
        try {
            // Extract first and second names
            String[] nombres = personDTO.getNombres().split(" ");
            String primerNombre = nombres[0];
            String segundoNombre = nombres.length > 1 ? nombres[1] : "";

            logger.debug("📝 Creating tracking record for person: {} {} {} {}",
                primerNombre, segundoNombre, 
                personDTO.getApellidoPaterno(), personDTO.getApellidoMaterno());

            // Create new PersonaProcesada with initial state
            PersonaProcesada personaProcesada = new PersonaProcesada(
                primerNombre,
                segundoNombre,
                personDTO.getApellidoPaterno(),
                personDTO.getApellidoMaterno()
            );

            // Set initial processing flags
            personaProcesada.setProcesado(false);
            personaProcesada.setTribunalPrincipalProcesado(false);
            personaProcesada.setVecesRevalidado(0);

            // Save to database
            PersonaProcesada savedPersona = personaProcesadaRepository.save(personaProcesada);

            logger.info("✅ PersonaProcesada created: {} {} {} {} (procesado=false, tribunal_principal_procesado=false)",
                primerNombre, segundoNombre,
                personDTO.getApellidoPaterno(), personDTO.getApellidoMaterno());

            return savedPersona;

        } catch (Exception e) {
            logger.error("❌ Error saving PersonaProcesada: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save PersonaProcesada", e);
        }
    }

    /**
     * Check if a PersonaProcesada already exists
     * 
     * @param personDTO The person to check
     * @return true if PersonaProcesada exists, false otherwise
     */
    public boolean personaProcesadaExists(PersonaDTO personDTO) {
        try {
            String[] nombres = personDTO.getNombres().split(" ");
            String primerNombre = nombres[0];
            String segundoNombre = nombres.length > 1 ? nombres[1] : "";

            Optional<PersonaProcesada> existing = personaProcesadaRepository
                .findByPrimerNombreAndSegundoNombreAndApellidoPaternoAndApellidoMaterno(
                    primerNombre,
                    segundoNombre,
                    personDTO.getApellidoPaterno(),
                    personDTO.getApellidoMaterno());

            return existing.isPresent();

        } catch (Exception e) {
            logger.warn("⚠️  Error checking if PersonaProcesada exists: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get or create PersonaProcesada for a person
     * If exists, return existing; if not, create new one
     * 
     * @param personDTO The person data
     * @return The PersonaProcesada (existing or newly created)
     */
    public PersonaProcesada getOrCreatePersonaProcesada(PersonaDTO personDTO) {
        try {
            String[] nombres = personDTO.getNombres().split(" ");
            String primerNombre = nombres[0];
            String segundoNombre = nombres.length > 1 ? nombres[1] : "";

            // Try to find existing PersonaProcesada
            Optional<PersonaProcesada> existing = personaProcesadaRepository
                .findByPrimerNombreAndSegundoNombreAndApellidoPaternoAndApellidoMaterno(
                    primerNombre,
                    segundoNombre,
                    personDTO.getApellidoPaterno(),
                    personDTO.getApellidoMaterno());

            if (existing.isPresent()) {
                logger.debug("⏭️  PersonaProcesada already exists for: {} {} {} {}",
                    primerNombre, segundoNombre,
                    personDTO.getApellidoPaterno(), personDTO.getApellidoMaterno());
                return existing.get();
            }

            // Create new PersonaProcesada if not found
            return saveNewPersonaProcesada(personDTO);

        } catch (Exception e) {
            logger.error("❌ Error getting or creating PersonaProcesada: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get or create PersonaProcesada", e);
        }
    }

    /**
     * Mark a PersonaProcesada as processed
     * 
     * @param personaProcesada The PersonaProcesada to update
     */
    public void markAsProcessed(PersonaProcesada personaProcesada) {
        try {
            personaProcesada.setProcesado(true);
            personaProcesadaRepository.save(personaProcesada);

            logger.debug("✅ PersonaProcesada marked as processed: {} {} {} {}",
                personaProcesada.getPrimerNombre(), personaProcesada.getSegundoNombre(),
                personaProcesada.getApellidoPaterno(), personaProcesada.getApellidoMaterno());

        } catch (Exception e) {
            logger.error("❌ Error marking PersonaProcesada as processed: {}", e.getMessage(), e);
        }
    }

    /**
     * Mark tribunal principal as processed for a PersonaProcesada
     * 
     * @param personaProcesada The PersonaProcesada to update
     */
    public void markTribunalPrincipalAsProcessed(PersonaProcesada personaProcesada) {
        try {
            personaProcesada.setTribunalPrincipalProcesado(true);
            personaProcesadaRepository.save(personaProcesada);

            logger.debug("✅ PersonaProcesada tribunal_principal marked as processed: {} {} {} {}",
                personaProcesada.getPrimerNombre(), personaProcesada.getSegundoNombre(),
                personaProcesada.getApellidoPaterno(), personaProcesada.getApellidoMaterno());

        } catch (Exception e) {
            logger.error("❌ Error marking tribunal principal as processed: {}", e.getMessage(), e);
        }
    }
}

