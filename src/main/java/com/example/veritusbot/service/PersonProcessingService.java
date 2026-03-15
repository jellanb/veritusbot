package com.example.veritusbot.service;

import com.example.veritusbot.dto.PersonaDTO;
import com.example.veritusbot.model.PersonaProcesada;
import com.example.veritusbot.repository.PersonaProcesadaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service responsible for managing person processing states
 * Handles filtering and marking people as processed
 * Implements configurable reprocessing logic (6 months for Phase 1, 12 for Phase 2)
 */
@Service
public class PersonProcessingService {
    private static final Logger logger = LoggerFactory.getLogger(PersonProcessingService.class);

    private final PersonaProcesadaRepository personaProcesadaRepository;

    @Value("${app.reprocess.phase1.months:6}")
    private int phase1ReprocessMonths;

    @Value("${app.reprocess.phase2.months:12}")
    private int phase2ReprocessMonths;

    public PersonProcessingService(PersonaProcesadaRepository personaProcesadaRepository) {
        this.personaProcesadaRepository = personaProcesadaRepository;
    }

    /**
     * Initialize - Log reprocessing configuration
     */
    public void init() {
        logger.info("📋 Reprocessing Configuration loaded:");
        logger.info("   Phase 1: {} months", phase1ReprocessMonths);
        logger.info("   Phase 2: {} months", phase2ReprocessMonths);
    }

    /**
     * Filter people for Phase 1: tribunal principal processing
     * Only processes people with tribunal_principal_procesado = false or null
     * 
     * @param people List of all people to filter
     * @return Filtered list ready for Phase 1
     */
    public List<PersonaDTO> filterPeopleForPhase1(List<PersonaDTO> people) {
        List<PersonaDTO> phase1People = new ArrayList<>();
        
        for (PersonaDTO person : people) {
            if (shouldProcessInPhase1(person)) {
                phase1People.add(person);
                logger.debug("👤 {} will be processed in Phase 1", person.getNombres());
            } else {
                logger.debug("⏭️  Skipping {} - tribunal principal already processed", person.getNombres());
            }
        }
        
        logger.info("📋 Phase 1: {} people will be processed (out of {} total)", 
            phase1People.size(), people.size());
        return phase1People;
    }

    /**
     * Filter people for Phase 2: general processing
     * Only processes people with procesado = false or null
     * 
     * @param people List of all people to filter
     * @return Filtered list ready for Phase 2
     */
    public List<PersonaDTO> filterPeopleForPhase2(List<PersonaDTO> people) {
        List<PersonaDTO> phase2People = new ArrayList<>();
        
        for (PersonaDTO person : people) {
            if (shouldProcessInPhase2(person)) {
                phase2People.add(person);
                logger.debug("👤 {} will be processed in Phase 2", person.getNombres());
            } else {
                logger.debug("⏭️  Skipping {} - already fully processed", person.getNombres());
            }
        }
        
        logger.info("📋 Phase 2: {} people will be processed (out of {} total)", 
            phase2People.size(), people.size());
        return phase2People;
    }

    /**
     * Mark all people as processed in Phase 1 (tribunal principal)
     * 
     * @param people List of people processed in Phase 1
     */
    public void markPhase1Complete(List<PersonaDTO> people) {
        for (PersonaDTO person : people) {
            PersonaProcesada procesada = findOrCreatePersonaProcesada(person);
            procesada.setTribunalPrincipalProcesado(true);
            personaProcesadaRepository.save(procesada);
            logger.debug("✅ Marked tribunal principal as processed for: {}", person.getNombres());
        }
    }

    /**
     * Mark all people as fully processed (Phase 2)
     * 
     * @param people List of people processed in Phase 2
     */
    public void markPhase2Complete(List<PersonaDTO> people) {
        for (PersonaDTO person : people) {
            PersonaProcesada procesada = findOrCreatePersonaProcesada(person);
            procesada.setProcesado(true);
            procesada.setFechaProcesada(LocalDateTime.now());
            personaProcesadaRepository.save(procesada);
            logger.debug("✅ Marked fully processed for: {}", person.getNombres());
        }
    }

    /**
     * Check if a person should be processed in Phase 1
     * Process if:
     * 1. Not in database (new person)
     * 2. tribunal_principal_procesado = false/null (never processed)
     * 3. fecha_procesada > phase1ReprocessMonths ago (needs reprocessing)
     *
     * @param person Person to check
     * @return true if person should be processed in Phase 1
     */
    private boolean shouldProcessInPhase1(PersonaDTO person) {
        Optional<PersonaProcesada> existing = findPersonaProcesada(person);
        
        if (existing.isEmpty()) {
            // Person not in database, process in Phase 1
            return true;
        }
        
        PersonaProcesada persona = existing.get();

        // Check if needs reprocessing (configured months passed)
        if (persona.getFechaProcesada() != null) {
            long monthsSinceProcesing = ChronoUnit.MONTHS.between(persona.getFechaProcesada(), LocalDateTime.now());
            if (monthsSinceProcesing >= phase1ReprocessMonths) {
                logger.debug("🔄 Reprocessing needed for {}: {} months since last processing (Phase 1 threshold: {})",
                        person.getNombres(), monthsSinceProcesing, phase1ReprocessMonths);
                persona.setUltimaRevalidacion(LocalDateTime.now());
                persona.setVecesRevalidado((persona.getVecesRevalidado() != null ? persona.getVecesRevalidado() : 0) + 1);
                personaProcesadaRepository.save(persona);
                return true;
            }
        }
        
        // Check tribunal principal flag
        if (persona.getTribunalPrincipalProcesado() == null || !persona.getTribunalPrincipalProcesado()) {
            return true;
        }

        return false;
    }

    /**
     * Check if a person should be processed in Phase 2
     * Process if:
     * 1. Not in database (new person)
     * 2. procesado = false/null (never fully processed)
     * 3. fecha_procesada > phase2ReprocessMonths ago (needs reprocessing)
     * 
     * @param person Person to check
     * @return true if person should be processed in Phase 2
     */
    private boolean shouldProcessInPhase2(PersonaDTO person) {
        Optional<PersonaProcesada> existing = findPersonaProcesada(person);
        
        if (existing.isEmpty()) {
            // Person not in database, process in Phase 2
            return true;
        }
        
        PersonaProcesada persona = existing.get();

        // Check if needs reprocessing (configured months passed)
        if (persona.getFechaProcesada() != null) {
            long monthsSinceProcesing = ChronoUnit.MONTHS.between(persona.getFechaProcesada(), LocalDateTime.now());
            if (monthsSinceProcesing >= phase2ReprocessMonths) {
                logger.debug("🔄 Reprocessing needed for {}: {} months since last processing (Phase 2 threshold: {})",
                        person.getNombres(), monthsSinceProcesing, phase2ReprocessMonths);
                persona.setUltimaRevalidacion(LocalDateTime.now());
                persona.setVecesRevalidado((persona.getVecesRevalidado() != null ? persona.getVecesRevalidado() : 0) + 1);
                personaProcesadaRepository.save(persona);
                return true;
            }
        }
        
        // Check procesado flag
        if (persona.getProcesado() == null || !persona.getProcesado()) {
            return true;
        }

        return false;
    }

    /**
     * Find a person in the database
     * 
     * @param person Person to find
     * @return Optional containing the PersonaProcesada if found
     */
    private Optional<PersonaProcesada> findPersonaProcesada(PersonaDTO person) {
        String primerNombre = person.getNombres().split(" ")[0];
        String segundoNombre = person.getNombres().split(" ").length > 1 ? 
                               person.getNombres().split(" ")[1] : "";
        
        return personaProcesadaRepository
                .findByPrimerNombreAndSegundoNombreAndApellidoPaternoAndApellidoMaterno(
                        primerNombre,
                        segundoNombre,
                        person.getApellidoPaterno(),
                        person.getApellidoMaterno());
    }

    /**
     * Find or create a PersonaProcesada record
     * 
     * @param person Person to find or create
     * @return PersonaProcesada instance
     */
    private PersonaProcesada findOrCreatePersonaProcesada(PersonaDTO person) {
        Optional<PersonaProcesada> existing = findPersonaProcesada(person);
        
        if (existing.isPresent()) {
            return existing.get();
        }
        
        // Create new PersonaProcesada
        String primerNombre = person.getNombres().split(" ")[0];
        String segundoNombre = person.getNombres().split(" ").length > 1 ? 
                               person.getNombres().split(" ")[1] : "";
        
        PersonaProcesada nuevaPersona = new PersonaProcesada(
                primerNombre,
                segundoNombre,
                person.getApellidoPaterno(),
                person.getApellidoMaterno()
        );
        
        return personaProcesadaRepository.save(nuevaPersona);
    }
}

