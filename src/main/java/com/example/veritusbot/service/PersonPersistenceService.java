package com.example.veritusbot.service;

import com.example.veritusbot.dto.PersonaDTO;
import com.example.veritusbot.model.Persona;
import com.example.veritusbot.repository.PersonaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.Optional;

/**
 * Service responsible for managing person persistence without duplicates
 * Implements Single Responsibility Principle: only handles person deduplication and persistence
 */
@Service
public class PersonPersistenceService {
    private static final Logger logger = LoggerFactory.getLogger(PersonPersistenceService.class);

    private final PersonaRepository personaRepository;

    public PersonPersistenceService(PersonaRepository personaRepository) {
        this.personaRepository = personaRepository;
    }

    /**
     * Save or get existing person from database
     * Prevents duplicate persons from being stored
     * 
     * @param personDTO Person to save or find
     * @return Saved or existing person
     */
    public Persona saveOrGetExisting(PersonaDTO personDTO) {
        try {
            // Extract names from PersonaDTO
            String[] nombres = personDTO.getNombres().split(" ");
            String primerNombre = nombres[0];
            String segundoNombre = nombres.length > 1 ? nombres[1] : "";

            logger.debug("🔍 Checking if person exists: {} {} {} {}", 
                primerNombre, segundoNombre, personDTO.getApellidoPaterno(), personDTO.getApellidoMaterno());

            // Check if person already exists
            Optional<Persona> existingPersona = personaRepository
                    .findByPrimerNombreAndSegundoNombreAndApellidoPaternoAndApellidoMaterno(
                            primerNombre,
                            segundoNombre,
                            personDTO.getApellidoPaterno(),
                            personDTO.getApellidoMaterno());

            if (existingPersona.isPresent()) {
                logger.debug("⏭️  Person already exists: {}", personDTO.getNombres());
                return existingPersona.get();
            }

            // Create new person if not found
            Persona newPersona = new Persona(
                    primerNombre,
                    segundoNombre,
                    personDTO.getApellidoPaterno(),
                    personDTO.getApellidoMaterno()
            );

            Persona savedPersona = personaRepository.save(newPersona);
            logger.info("✅ New person saved: {} {} {} {}", 
                primerNombre, segundoNombre, 
                personDTO.getApellidoPaterno(), personDTO.getApellidoMaterno());

            return savedPersona;

        } catch (Exception e) {
            logger.error("❌ Error saving or getting person: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save or get person", e);
        }
    }

    /**
     * Check if a person exists in database
     * 
     * @param personDTO Person to check
     * @return true if person exists, false otherwise
     */
    public boolean personExists(PersonaDTO personDTO) {
        try {
            String[] nombres = personDTO.getNombres().split(" ");
            String primerNombre = nombres[0];
            String segundoNombre = nombres.length > 1 ? nombres[1] : "";

            return personaRepository
                    .findByPrimerNombreAndSegundoNombreAndApellidoPaternoAndApellidoMaterno(
                            primerNombre,
                            segundoNombre,
                            personDTO.getApellidoPaterno(),
                            personDTO.getApellidoMaterno())
                    .isPresent();

        } catch (Exception e) {
            logger.warn("⚠️  Error checking if person exists: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get person from database if exists
     * 
     * @param personDTO Person to find
     * @return Optional containing person if found
     */
    public Optional<Persona> findPerson(PersonaDTO personDTO) {
        try {
            String[] nombres = personDTO.getNombres().split(" ");
            String primerNombre = nombres[0];
            String segundoNombre = nombres.length > 1 ? nombres[1] : "";

            return personaRepository
                    .findByPrimerNombreAndSegundoNombreAndApellidoPaternoAndApellidoMaterno(
                            primerNombre,
                            segundoNombre,
                            personDTO.getApellidoPaterno(),
                            personDTO.getApellidoMaterno());

        } catch (Exception e) {
            logger.warn("⚠️  Error finding person: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Get duplicate count for a person
     * Useful for statistics
     * 
     * @param personDTO Person to count duplicates
     * @return Number of times person would be duplicated
     */
    public int getDuplicateCount(PersonaDTO personDTO) {
        return personExists(personDTO) ? 1 : 0;
    }
}

