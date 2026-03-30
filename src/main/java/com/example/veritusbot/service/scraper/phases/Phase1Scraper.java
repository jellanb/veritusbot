package com.example.veritusbot.service.scraper.phases;

import com.example.veritusbot.dto.PersonaDTO;
import com.example.veritusbot.dto.ResultDTO;
import com.example.veritusbot.service.ResultPersistenceService;
import com.example.veritusbot.service.scraper.browser.BrowserManager;
import com.example.veritusbot.service.scraper.browser.FrameNavigator;
import com.example.veritusbot.service.scraper.browser.HumanBehaviorService;
import com.example.veritusbot.service.scraper.form.FormFiller;
import com.example.veritusbot.service.scraper.form.TribunalSelector;
import com.example.veritusbot.service.scraper.parser.ResultParser;
import com.microsoft.playwright.Frame;
import com.microsoft.playwright.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.*;
import com.example.veritusbot.service.scraper.retry.RetryableScraperException;

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
    private final HumanBehaviorService humanBehaviorService;
    private final BrowserManager browserManager;

    public Phase1Scraper(FormFiller formFiller, TribunalSelector tribunalSelector,
                         ResultParser resultParser, FrameNavigator frameNavigator,
                         ResultPersistenceService resultPersistenceService,
                         HumanBehaviorService humanBehaviorService,
                         BrowserManager browserManager) {
        this.formFiller = formFiller;
        this.tribunalSelector = tribunalSelector;
        this.resultParser = resultParser;
        this.frameNavigator = frameNavigator;
        this.resultPersistenceService = resultPersistenceService;
        this.humanBehaviorService = humanBehaviorService;
        this.browserManager = browserManager;
    }

    @Override
    public List<ResultDTO> execute(Page page, PersonaDTO person, int startYear, int endYear, int startTribunalPosition)
            throws RetryableScraperException {
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
            humanBehaviorService.pauseShort(page);
            Map<String, Integer> allTribunals = tribunalSelector.loadAllTribunals(searchFrame);

            tribunalSelector.closeDropdown(searchFrame);
            humanBehaviorService.pauseShort(page);

            // Filter to only Santiago tribunals (1-30)
            List<Map.Entry<String, Integer>> santiagoTribunals = filterSantiagoTribunals(allTribunals);

            if (santiagoTribunals.isEmpty()) {
                logger.warn("⚠ No Santiago tribunals found");
                return results;
            }

            int resumePosition = Math.max(0, startTribunalPosition);
            if (resumePosition >= santiagoTribunals.size()) {
                logger.warn("⚠ Resume position {} is out of range for {} tribunals", resumePosition, santiagoTribunals.size());
                return results;
            }

            logger.info("📋 Phase 1: Found {} Santiago tribunals (resume from position {})", santiagoTribunals.size(), resumePosition);

            // Search in each Santiago tribunal
            for (int tribunalPosition = resumePosition; tribunalPosition < santiagoTribunals.size(); tribunalPosition++) {
                Map.Entry<String, Integer> tribunalEntry = santiagoTribunals.get(tribunalPosition);
                String tribunalName = tribunalEntry.getKey();
                try {
                    Integer tribunalIndex = tribunalEntry.getValue();
                    if (tribunalIndex == null) {
                        continue;
                    }

                    logger.debug("🔍 Searching in: {} (index: {}, proxy: {})",
                            tribunalName,
                            tribunalIndex,
                            browserManager.getProxyLabel(page));

                    // Open dropdown
                    tribunalSelector.openDropdown(searchFrame);
                    humanBehaviorService.pauseShort(page);

                    // Select tribunal
                    tribunalSelector.selectTribunal(searchFrame, tribunalName, tribunalIndex);
                    humanBehaviorService.pauseShort(page);

                    // Submit search
                    formFiller.submitForm(searchFrame);
                    humanBehaviorService.waitForDomAndNetwork(page);
                    humanBehaviorService.pause(page, 600, 1600);

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

                } catch (Exception e) {
                    // ✅ Throw exception with context for ScraperOrchestrator to handle retries
                    String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                    logger.error("❌ Error searching tribunal {}: {}", tribunalName, errorMsg);
                    
                    // If it's a browser/network error, let ScraperOrchestrator know to retry
                    // Otherwise, log and continue to next tribunal
                    if (RetryableScraperException.isBrowserOrNetworkError(e)) {
                        throw new com.example.veritusbot.service.scraper.retry.RetryableScraperException(
                            "Browser/Network error while searching tribunal: " + tribunalName,
                            e,
                            true,  // isRetryable
                            "tribunal: " + tribunalName,
                            tribunalPosition
                        );
                    } else {
                        logger.warn("⚠ Non-retryable error in tribunal {}, continuing to next...", tribunalName);
                    }
                }
            }

            logger.info("✓ Phase 1 completed. Found {} results", results.size());

        } catch (Exception e) {
            logger.error("❌ Error in Phase 1: ", e);
            throw e;  // ✅ Let ScraperOrchestrator handle the exception
        }

        return results;
    }

    /**
     * Filter tribunals that contain "Santiago" in the name
     */
    private List<Map.Entry<String, Integer>> filterSantiagoTribunals(Map<String, Integer> allTribunals) {
        List<Map.Entry<String, Integer>> santiago = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : allTribunals.entrySet()) {
            String name = entry.getKey();
            if (name.contains("Santiago") && !name.contains("Seleccione")) {
                santiago.add(entry);
            }
        }
        santiago.sort(Comparator.comparingInt(entry -> entry.getValue() != null ? entry.getValue() : Integer.MAX_VALUE));
        return santiago;
    }


    @Override
    public String getPhaseName() {
        return "Phase 1: Santiago Tribunals (1-30)";
    }
}












