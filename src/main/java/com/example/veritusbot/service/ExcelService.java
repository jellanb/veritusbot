package com.example.veritusbot.service;

import com.example.veritusbot.dto.PersonaDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExcelService {
    private static final Logger logger = LoggerFactory.getLogger(ExcelService.class);

    private final PersonPersistenceService personPersistenceService;

    public ExcelService(PersonPersistenceService personPersistenceService) {
        this.personPersistenceService = personPersistenceService;
    }

    /**
     * Read a CSV file and return a list of PersonaDTO
     * Also saves each person to database, preventing duplicates
     * File must be located in project root
     * CSV Format: Names;Last Name;Mother's Last Name;StartYear;EndYear
     * (Uses semicolon as separator)
     *
     * @param fileName CSV file name (ex: "personas.csv")
     * @return List of PersonaDTO objects
     */
    public List<PersonaDTO> readClientFromCSV(String fileName) {
        List<PersonaDTO> people = new ArrayList<>();
        int newPersonasCount = 0;
        int duplicatePersonasCount = 0;

        try {
            // File path in project root
            File file = new File(fileName);

            if (!file.exists()) {
                System.err.println("❌ File not found: " + fileName);
                System.err.println("   Expected path: " + file.getAbsolutePath());
                return people;
            }

            System.out.println("📖 Reading file: " + fileName);
            System.out.println("   Path: " + file.getAbsolutePath());

            // Read CSV file
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                int lineNumber = 0;

                while ((line = reader.readLine()) != null) {
                    lineNumber++;

                    // Skip header
                    if (lineNumber == 1) {
                        System.out.println("📊 Header found: " + line);
                        continue;
                    }

                    try {
                        // Parse CSV line
                        String[] values = parseCSV(line);

                        if (values.length >= 5) {
                            String names = values[0].trim();
                            String lastNamePaternal = values[1].trim();
                            String lastNameMaternal = values[2].trim();
                            int startYear;
                            int endYear;

                            try {
                                startYear = Integer.parseInt(values[3].trim());
                            } catch (NumberFormatException e) {
                                System.err.println("⚠ Error in StartYear (line " + lineNumber + "): " + e.getMessage());
                                continue;
                            }

                            try {
                                endYear = Integer.parseInt(values[4].trim());
                            } catch (NumberFormatException e) {
                                System.err.println("⚠ Error in EndYear (line " + lineNumber + "): " + e.getMessage());
                                continue;
                            }

                            if (!names.isEmpty() && !lastNamePaternal.isEmpty() && !lastNameMaternal.isEmpty() && startYear > 0 && endYear > 0) {
                                PersonaDTO person = new PersonaDTO(names, lastNamePaternal, lastNameMaternal, startYear, endYear);
                                people.add(person);

                                // 💾 Save to database, preventing duplicates
                                if (personPersistenceService.personExists(person)) {
                                    System.out.println("⏭️  Duplicate: " + person + " (already in database)");
                                    duplicatePersonasCount++;
                                } else {
                                    personPersistenceService.saveOrGetExisting(person);
                                    System.out.println("✓ Person saved and loaded: " + person);
                                    newPersonasCount++;
                                }
                            } else {
                                System.err.println("⚠ Line " + lineNumber + " incomplete or has invalid years");
                            }
                        } else {
                            System.err.println("⚠ Line " + lineNumber + " has incorrect format (expected 5 columns, found " + values.length + ")");
                        }
                    } catch (Exception e) {
                        System.err.println("❌ Error processing line " + lineNumber + ": " + e.getMessage());
                    }
                }
            }

            System.out.println("\n📊 Summary:");
            System.out.println("✓ Total people loaded: " + people.size());
            System.out.println("✅ New people saved to database: " + newPersonasCount);
            System.out.println("⏭️  Duplicates skipped: " + duplicatePersonasCount);

        } catch (IOException e) {
            System.err.println("❌ Error reading file: " + e.getMessage());
        }

        return people;
    }

    /**
     * Parse a CSV line using semicolon as separator
     * Format: Names;Last Name;Mother's Last Name;StartYear;EndYear
     */
    private String[] parseCSV(String line) {
        // Split by semicolon (;)
        String[] values = line.split(";");

        // Trim each value
        for (int i = 0; i < values.length; i++) {
            values[i] = values[i].trim();
        }

        return values;
    }
}
