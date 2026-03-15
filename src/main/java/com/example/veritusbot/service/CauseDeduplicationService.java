package com.example.veritusbot.service;

import com.example.veritusbot.dto.PersonaDTO;
import com.example.veritusbot.dto.ResultDTO;
import com.example.veritusbot.model.Causa;
import com.example.veritusbot.repository.CausaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.Optional;
import java.util.UUID;

/**
 * Service responsible for managing causa (result) persistence without duplicates
 * Implements Single Responsibility Principle: only handles causa deduplication and persistence
 */
@Service
public class CauseDeduplicationService {
    private static final Logger logger = LoggerFactory.getLogger(CauseDeduplicationService.class);

    private final CausaRepository causaRepository;
    private final PersonPersistenceService personPersistenceService;

    public CauseDeduplicationService(CausaRepository causaRepository, 
                                      PersonPersistenceService personPersistenceService) {
        this.causaRepository = causaRepository;
        this.personPersistenceService = personPersistenceService;
    }

    /**
     * Save result (causa) if it doesn't already exist
     * Prevents duplicate causas from being stored
     * 
     * @param result Result to save
     * @param person Associated person
     * @return true if saved, false if duplicate
     */
    public boolean saveIfNotDuplicate(ResultDTO result, PersonaDTO person) {
        try {
            // Check if causa already exists
            if (causaExists(result)) {
                logger.debug("⏭️  Duplicate causa skipped: {} - {} ({})",
                    result.getResolution(), result.getTribunal(), result.getYear());
                return false;
            }

            // Causa doesn't exist, save it
            Causa causa = new Causa();
            causa.setPersonaId(getOrCreatePersonId(person));
            causa.setRol(result.getResolution());
            causa.setAnio(result.getYear());
            causa.setCaratula(result.getDetails());
            causa.setTribunal(result.getTribunal());

            causaRepository.save(causa);
            logger.info("✅ Causa saved: {} - {} ({})",
                result.getResolution(), result.getTribunal(), result.getYear());

            return true;

        } catch (Exception e) {
            logger.error("❌ Error saving causa: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save causa", e);
        }
    }

    /**
     * Check if a causa already exists in database
     * 
     * @param result Result to check
     * @return true if causa exists, false otherwise
     */
    public boolean causaExists(ResultDTO result) {
        try {
            return causaRepository.existsByRolAndAnioAndTribunal(
                    result.getResolution(),
                    result.getYear(),
                    result.getTribunal());

        } catch (Exception e) {
            logger.warn("⚠️  Error checking if causa exists: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Find a causa in database if it exists
     * 
     * @param result Result to find
     * @return Optional containing causa if found
     */
    public Optional<Causa> findCausa(ResultDTO result) {
        try {
            return causaRepository.findByRolAndAnioAndTribunal(
                    result.getResolution(),
                    result.getYear(),
                    result.getTribunal());

        } catch (Exception e) {
            logger.warn("⚠️  Error finding causa: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Get or create persona ID for the result
     * 
     * @param person The person associated with result
     * @return UUID of the person
     */
    private UUID getOrCreatePersonId(PersonaDTO person) {
        try {
            Optional<com.example.veritusbot.model.Persona> existingPerson = 
                personPersistenceService.findPerson(person);
            
            if (existingPerson.isPresent()) {
                return existingPerson.get().getId();
            }

            // Create new person if not found
            com.example.veritusbot.model.Persona savedPerson = 
                personPersistenceService.saveOrGetExisting(person);
            return savedPerson.getId();

        } catch (Exception e) {
            logger.error("❌ Error getting or creating person ID: {}", e.getMessage());
            // Return a generated UUID if error occurs
            return UUID.randomUUID();
        }
    }

    /**
     * Get statistics about duplicate causas prevented
     * 
     * @param results List of results to analyze
     * @return Number of duplicates that would have been saved
     */
    public int getDuplicateCount(java.util.List<ResultDTO> results) {
        int duplicateCount = 0;
        for (ResultDTO result : results) {
            if (causaExists(result)) {
                duplicateCount++;
            }
        }
        return duplicateCount;
    }
}

