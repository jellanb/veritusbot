package com.example.veritusbot.service;

import com.example.veritusbot.dto.PersonaDTO;
import com.example.veritusbot.dto.ResultDTO;
import com.example.veritusbot.model.Causa;
import com.example.veritusbot.repository.CausaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

/**
 * Service responsible for persisting search results to both CSV file and database
 * Implements Single Responsibility Principle: only handles result persistence
 */
@Service
public class ResultPersistenceService {
    private static final Logger logger = LoggerFactory.getLogger(ResultPersistenceService.class);
    
    private static final String CSV_FILE_NAME = "resultados_busqueda.csv";
    private static final String CSV_HEADER = "Persona,Tribunal,Año,Caratula,Rol\n";

    private final CausaRepository causaRepository;

    public ResultPersistenceService(CausaRepository causaRepository) {
        this.causaRepository = causaRepository;
    }

    /**
     * Save a single result to CSV file and database
     * Called immediately after a result is found during scraping
     * 
     * @param result The result to save
     * @param person The person associated with this result
     */
    public void saveResult(ResultDTO result, PersonaDTO person) {
        try {
            logger.debug("💾 Saving result for person: {}", person.getNombres());

            // Save to CSV file
            saveToCSV(result);

            // Save to database
            saveToDatabase(result, person);

            logger.debug("✅ Result saved successfully");

        } catch (Exception e) {
            logger.error("❌ Error saving result: {}", e.getMessage(), e);
        }
    }

    /**
     * Save multiple results
     * 
     * @param results List of results to save
     * @param person The person associated with these results
     */
    public void saveResults(List<ResultDTO> results, PersonaDTO person) {
        if (results == null || results.isEmpty()) {
            return;
        }

        for (ResultDTO result : results) {
            saveResult(result, person);
        }
    }

    /**
     * Save result to CSV file (append mode)
     * Creates file if it doesn't exist, with header on first write
     * 
     * @param result The result to save
     * @throws IOException If file operation fails
     */
    private void saveToCSV(ResultDTO result) throws IOException {
        File file = new File(CSV_FILE_NAME);
        boolean fileExists = file.exists() && file.length() > 0;

        try (FileWriter fw = new FileWriter(file, true);
             BufferedWriter bw = new BufferedWriter(fw)) {

            // Write header if file is new
            if (!fileExists) {
                bw.write(CSV_HEADER);
                logger.debug("📝 Created CSV file with header");
            }

            // Format and write result line
            String csvLine = formatResultAsCSV(result);
            bw.write(csvLine);
            bw.newLine();

            logger.debug("📝 Result appended to CSV: {} ({})", result.getPersonName(), result.getTribunal());

        } catch (IOException e) {
            logger.error("❌ Error writing to CSV file: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Format a ResultDTO as a CSV line
     * Format: Persona,Tribunal,Año,Caratula,Rol
     * 
     * @param result The result to format
     * @return Formatted CSV line
     */
    private String formatResultAsCSV(ResultDTO result) {
        return String.join(",",
                escapeCSV(result.getPersonName()),
                escapeCSV(result.getTribunal()),
                String.valueOf(result.getYear()),
                escapeCSV(result.getDetails()),   // Caratula/Details
                escapeCSV(result.getResolution()) // Rol/Resolution
        );
    }

    /**
     * Escape special characters in CSV field
     * Wraps field in quotes if it contains comma, quote, or newline
     * 
     * @param field The field to escape
     * @return Escaped field
     */
    private String escapeCSV(String field) {
        if (field == null) {
            return "";
        }

        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }

        return field;
    }

    /**
     * Save result to database
     * Creates a new Causa record
     * 
     * @param result The result to save
     * @param person The person associated with this result
     */
    private void saveToDatabase(ResultDTO result, PersonaDTO person) {
        try {
            Causa causa = new Causa();
            causa.setPersonaId(getOrCreatePersonId(person));
            causa.setRol(result.getResolution());
            causa.setAnio(result.getYear());
            causa.setCaratula(result.getDetails());
            causa.setTribunal(result.getTribunal());

            causaRepository.save(causa);
            logger.debug("💾 Cause saved to database: {} in {}", result.getResolution(), result.getTribunal());

        } catch (Exception e) {
            logger.error("❌ Error saving to database: {}", e.getMessage());
            throw new RuntimeException("Failed to save result to database", e);
        }
    }

    /**
     * Get or create a PersonId for the result
     * This is a simplified version - in production, you'd link to actual Persona records
     * 
     * @param person The person DTO
     * @return A UUID for the person
     */
    private UUID getOrCreatePersonId(PersonaDTO person) {
        // For now, generate a deterministic UUID based on person data
        // In production, this should query PersonaRepository
        String personKey = person.getNombres() + person.getApellidoPaterno() + person.getApellidoMaterno();
        return UUID.nameUUIDFromBytes(personKey.getBytes());
    }

    /**
     * Clear the CSV results file (useful for fresh start)
     * Creates a new file with only header
     */
    public void clearResults() {
        try {
            Files.write(Paths.get(CSV_FILE_NAME), CSV_HEADER.getBytes());
            logger.info("🗑️  Results file cleared");
        } catch (IOException e) {
            logger.warn("⚠️  Error clearing results file: {}", e.getMessage());
        }
    }

    /**
     * Get the path to the results CSV file
     * 
     * @return Path to CSV file
     */
    public String getResultsFilePath() {
        try {
            return new File(CSV_FILE_NAME).getAbsolutePath();
        } catch (Exception e) {
            return CSV_FILE_NAME;
        }
    }
}

