package com.example.veritusbot.service.scraper.phases;

import com.example.veritusbot.dto.PersonaDTO;
import com.example.veritusbot.dto.ResultDTO;
import com.example.veritusbot.service.ResultPersistenceService;
import com.example.veritusbot.service.scraper.browser.FrameNavigator;
import com.example.veritusbot.service.scraper.form.FormFiller;
import com.example.veritusbot.service.scraper.form.TribunalSelector;
import com.example.veritusbot.service.scraper.parser.ResultParser;
import com.microsoft.playwright.Frame;
import com.microsoft.playwright.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.*;

/**
 * Phase 1 Scraper: Search in Santiago tribunals (1º - 30º)
 * Persists results immediately when found (not at the end)
 */
@Component
public class Phase1Scraper implements Phase {
    private static final Logger logger = LoggerFactory.getLogger(Phase1Scraper.class);

    private final FormFiller formFiller;
    private final TribunalSelector tribunalSelector;
    private final ResultParser resultParser;
    private final FrameNavigator frameNavigator;
    private final ResultPersistenceService resultPersistenceService;

    public Phase1Scraper(FormFiller formFiller, TribunalSelector tribunalSelector,
                         ResultParser resultParser, FrameNavigator frameNavigator,
                         ResultPersistenceService resultPersistenceService) {
        this.formFiller = formFiller;
        this.tribunalSelector = tribunalSelector;
        this.resultParser = resultParser;
        this.frameNavigator = frameNavigator;
        this.resultPersistenceService = resultPersistenceService;
    }

    @Override
    public List<ResultDTO> execute(Page page, PersonaDTO person, int startYear, int endYear) {
        List<ResultDTO> results = new ArrayList<>();

        try {
            logger.info("▶️  Starting Phase 1 (Santiago tribunals 1-30)...");

            // Navigate to search form
            frameNavigator.navigateToSearchForm(page);

            // Get search frame
            Frame searchFrame = frameNavigator.getSearchFrame(page);
            if (searchFrame == null) {
                searchFrame = page.mainFrame();
            }

            // Click search by name tab
            frameNavigator.clickSearchByNameTab(searchFrame, page);

            // Set competence to Civil
            frameNavigator.setCompetenceToCivil(searchFrame, page);

            // Fill form with person data
            formFiller.fillSearchForm(searchFrame, person, startYear);

            // Open dropdown and load tribunals
            tribunalSelector.openDropdown(searchFrame);
            page.waitForTimeout(1500);
            Map<String, Integer> allTribunals = tribunalSelector.loadAllTribunals(searchFrame);

            tribunalSelector.closeDropdown(searchFrame);
            page.waitForTimeout(500);

            // Filter to only Santiago tribunals (1-30)
            List<String> santigoTribunals = filterSantiagoTribunals(allTribunals);

            if (santigoTribunals.isEmpty()) {
                logger.warn("⚠ No Santiago tribunals found");
                return results;
            }

            logger.info("📋 Phase 1: Found {} Santiago tribunals", santigoTribunals.size());

            // Search in each Santiago tribunal
            for (String tribunalName : santigoTribunals) {
                try {
                    Integer tribunalIndex = allTribunals.get(tribunalName);
                    if (tribunalIndex == null) {
                        continue;
                    }

                    // ✅ RETRY LOGIC: Max 5 retries per tribunal if browser closes
                    int maxRetries = 5;
                    int retryCount = 0;
                    boolean tribunalSuccess = false;
                    
                    while (retryCount < maxRetries && !tribunalSuccess) {
                        retryCount++;
                        
                        try {
                            logger.debug("🔍 Searching in: {} (index: {}) - Attempt {}/{}", 
                                tribunalName, tribunalIndex, retryCount, maxRetries);

                            // Open dropdown
                            tribunalSelector.openDropdown(searchFrame);
                            page.waitForTimeout(500);

                            // Select tribunal
                            tribunalSelector.selectTribunal(searchFrame, tribunalName, tribunalIndex);
                            page.waitForTimeout(2000);

                            // Submit search
                            formFiller.submitForm(searchFrame);
                            page.waitForTimeout(15000);

                            // Parse results
                            String html = page.content();
                            List<ResultDTO> tribunalResults = resultParser.parseResults(html, person, startYear, tribunalName);
                            results.addAll(tribunalResults);

                            if (!tribunalResults.isEmpty()) {
                                logger.debug("✅ Found {} results in {}", tribunalResults.size(), tribunalName);
                                
                                // ✅ PERSIST IMMEDIATELY WHEN FOUND (not at the end)
                                logger.debug("💾 Persisting {} results found in tribunal: {}", tribunalResults.size(), tribunalName);
                                resultPersistenceService.saveResults(tribunalResults, person);
                            }
                            
                            tribunalSuccess = true;  // ✅ Mark tribunal as successful

                        } catch (Exception e) {
                            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                            
                            // Check if it's a browser closure error
                            boolean isBrowserClosed = e.getMessage() != null && 
                                (e.getMessage().contains("Target page, context or browser has been closed") ||
                                 e.getMessage().contains("Target closed") ||
                                 e.getMessage().contains("Browser closed") ||
                                 e.getMessage().contains("Connection closed") ||
                                 e.getMessage().contains("Protocol error") ||
                                 e.getMessage().contains("WebSocket is closed") ||
                                 e.getMessage().contains("ERR_FAILED") ||
                                 e.getMessage().contains("net::ERR") ||
                                 e.getClass().getSimpleName().contains("PlaywrightException")) ||
                                e instanceof java.io.IOException;
                            
                            if (isBrowserClosed) {
                                // Browser was closed
                                if (retryCount < maxRetries) {
                                    logger.warn("   ⚠️  Browser closed while searching tribunal: {}. Retrying... (Attempt {}/{})",
                                        tribunalName, retryCount, maxRetries);
                                    
                                    // Exponential backoff: 1s, 2s, 4s, 8s, 16s
                                    long backoffMs = (long) Math.pow(2, retryCount - 1) * 1000;
                                    try {
                                        logger.debug("   ⏳ Waiting {}ms before retry...", backoffMs);
                                        Thread.sleep(backoffMs);
                                    } catch (InterruptedException ie) {
                                        Thread.currentThread().interrupt();
                                        logger.warn("   ⚠️  Retry wait interrupted");
                                        break;
                                    }
                                } else {
                                    logger.error("   ❌ Tribunal {} FAILED after {} attempts (browser closed). Skipping to next tribunal.",
                                        tribunalName, maxRetries);
                                }
                            } else {
                                // Other error
                                if (retryCount < maxRetries) {
                                    logger.error("   ❌ Error searching tribunal {}: {} (Attempt {}/{}). Retrying...",
                                        tribunalName, errorMsg, retryCount, maxRetries);
                                    
                                    // Exponential backoff
                                    long backoffMs = (long) Math.pow(2, retryCount - 1) * 1000;
                                    try {
                                        logger.debug("   ⏳ Waiting {}ms before retry...", backoffMs);
                                        Thread.sleep(backoffMs);
                                    } catch (InterruptedException ie) {
                                        Thread.currentThread().interrupt();
                                        logger.warn("   ⚠️  Retry wait interrupted");
                                        break;
                                    }
                                } else {
                                    logger.error("   ❌ Tribunal {} FAILED after {} attempts: {}. Skipping to next tribunal.",
                                        tribunalName, maxRetries, errorMsg);
                                }
                            }
                        }
                    }

                } catch (Exception e) {
                    logger.warn("⚠ Unexpected error processing tribunal {}: {}", tribunalName, e.getMessage());
                }
            }

            logger.info("✓ Phase 1 completed. Found {} results", results.size());

        } catch (Exception e) {
            logger.error("❌ Error in Phase 1: ", e);
        }

        return results;
    }

    /**
     * Filter tribunals that contain "Santiago" in the name
     */
    private List<String> filterSantiagoTribunals(Map<String, Integer> allTribunals) {
        List<String> santiago = new ArrayList<>();
        for (String name : allTribunals.keySet()) {
            if (name.contains("Santiago") && !name.contains("Seleccione")) {
                santiago.add(name);
            }
        }
        return santiago;
    }

    @Override
    public String getPhaseName() {
        return "Phase 1: Santiago Tribunals (1-30)";
    }
}





