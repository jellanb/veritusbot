package com.example.veritusbot.service.scraper.parser;

import com.example.veritusbot.dto.PersonaDTO;
import com.example.veritusbot.dto.ResultDTO;
import com.example.veritusbot.service.ResultPersistenceService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class ResultParser {
    private static final Logger logger = LoggerFactory.getLogger(ResultParser.class);

    private final ResultPersistenceService resultPersistenceService;

    public ResultParser(ResultPersistenceService resultPersistenceService) {
        this.resultPersistenceService = resultPersistenceService;
    }

    /**
     * Parse search results from HTML content
     * Saves results immediately to CSV and database as they are found
     * 
     * @param htmlContent HTML content to parse
     * @param person person searched
     * @param year Year of search
     * @param tribunal Tribunal name
     * @return List of results found
     */
    public List<ResultDTO> parseResults(String htmlContent, PersonaDTO person, int year, String tribunal) {
        List<ResultDTO> results = new ArrayList<>();

        try {
            logger.debug("📋 Parsing search results...");
            Document doc = Jsoup.parse(htmlContent);

            // Check for "no results" message
            Element noResultsMessage = doc.selectFirst("tbody#verDetalleNombre tr td[colspan='5']");
            if (noResultsMessage != null && noResultsMessage.text().contains("No se han encontrado resultados")) {
                logger.debug("ℹ️  No results found for: {}", person.getNombres());
                return results;
            }

            // Extract result rows
            Elements rows = doc.select("table#dtaTableDetalleNombre tbody#verDetalleNombre tr");
            logger.debug("Found {} rows to process", rows.size());

            for (Element row : rows) {
                // Skip rows with colspan (pagination, messages)
                if (!row.select("td[colspan]").isEmpty()) {
                    continue;
                }

                Elements cols = row.select("td");
                if (cols.size() >= 5) {
                    String resolution = cols.get(1).text().trim();
                    String date = cols.get(2).text().trim();
                    String description = cols.get(3).text().trim();
                    String tribunalValue = cols.get(4).text().trim();

                    ResultDTO result = new ResultDTO(
                        person.getNombres(),
                        tribunalValue,
                        year,
                        resolution,
                        description
                    );

                    results.add(result);
                    logger.debug("✅ Result added: {}", resolution);

                    // 💾 SAVE IMMEDIATELY to CSV and Database
                    resultPersistenceService.saveResult(result, person);
                }
            }

            if (!results.isEmpty()) {
                logger.info("✅ Found and saved {} results for {}", results.size(), person.getNombres());
            }
            logger.debug("✓ Parsing completed. Found {} results", results.size());

        } catch (Exception e) {
            logger.error("❌ Error parsing results: ", e);
        }

        return results;
    }

    /**
     * Check if there are results in the HTML
     * @param htmlContent HTML content to check
     * @return true if results exist
     */
    public boolean hasResults(String htmlContent) {
        try {
            Document doc = Jsoup.parse(htmlContent);
            Element noResultsMessage = doc.selectFirst("tbody#verDetalleNombre tr td[colspan='5']");
            return noResultsMessage == null || !noResultsMessage.text().contains("No se han encontrado resultados");
        } catch (Exception e) {
            logger.warn("⚠ Error checking for results: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extract all result data from HTML
     * @param htmlContent HTML content
     * @return List of result rows as arrays
     */
    public List<String[]> extractAllResults(String htmlContent) {
        List<String[]> results = new ArrayList<>();

        try {
            Document doc = Jsoup.parse(htmlContent);
            Elements rows = doc.select("table#dtaTableDetalleNombre tbody#verDetalleNombre tr");

            for (Element row : rows) {
                if (!row.select("td[colspan]").isEmpty()) {
                    continue;
                }

                Elements cols = row.select("td");
                if (cols.size() >= 5) {
                    String[] rowData = new String[]{
                        cols.get(1).text().trim(),  // Resolution
                        cols.get(2).text().trim(),  // Date
                        cols.get(3).text().trim(),  // Description
                        cols.get(4).text().trim()   // Tribunal
                    };
                    results.add(rowData);
                }
            }

        } catch (Exception e) {
            logger.error("❌ Error extracting results: ", e);
        }

        return results;
    }
}

