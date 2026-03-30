package com.example.veritusbot.service.scraper.phases;

import com.example.veritusbot.dto.PersonaDTO;
import com.example.veritusbot.dto.ResultDTO;
import com.example.veritusbot.service.ResultPersistenceService;
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
 * Phase 2 Scraper: Search in other tribunals (excluding Santiago)
 * Persists results immediately when found (not at the end)
 * Throws RetryableScraperException for browser/network errors that ScraperOrchestrator should retry
 */
@Component
public class Phase2Scraper implements Phase {
    private static final Logger logger = LoggerFactory.getLogger(Phase2Scraper.class);

    private final FormFiller formFiller;
    private final TribunalSelector tribunalSelector;
    private final ResultParser resultParser;
    private final FrameNavigator frameNavigator;
    private final ResultPersistenceService resultPersistenceService;
    private final HumanBehaviorService humanBehaviorService;

    public Phase2Scraper(FormFiller formFiller, TribunalSelector tribunalSelector,
                         ResultParser resultParser, FrameNavigator frameNavigator,
                         ResultPersistenceService resultPersistenceService,
                         HumanBehaviorService humanBehaviorService) {
        this.formFiller = formFiller;
        this.tribunalSelector = tribunalSelector;
        this.resultParser = resultParser;
        this.frameNavigator = frameNavigator;
        this.resultPersistenceService = resultPersistenceService;
        this.humanBehaviorService = humanBehaviorService;
    }

    @Override
    public List<ResultDTO> execute(Page page, PersonaDTO person, int startYear, int endYear)
            throws RetryableScraperException {
        List<ResultDTO> results = new ArrayList<>();

        try {
            logger.info("▶️  Starting Phase 2 (Other tribunals - excluding Santiago)...");

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

            // Filter to exclude Santiago tribunals
            List<String> otherTribunals = filterOtherTribunals(allTribunals);

            if (otherTribunals.isEmpty()) {
                logger.warn("⚠ No other tribunals found");
                return results;
            }

            logger.info("📋 Phase 2: Found {} other tribunals", otherTribunals.size());

            // Search in each tribunal
            for (String tribunalName : otherTribunals) {
                try {
                    Integer tribunalIndex = allTribunals.get(tribunalName);
                    if (tribunalIndex == null) {
                        continue;
                    }

                    logger.debug("🔍 Searching in: {} (index: {})", tribunalName, tribunalIndex);

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
                            "tribunal: " + tribunalName
                        );
                    } else {
                        logger.warn("⚠ Non-retryable error in tribunal {}, continuing to next...", tribunalName);
                    }
                }
            }

            logger.info("✓ Phase 2 completed. Found {} results", results.size());

        } catch (Exception e) {
            logger.error("❌ Error in Phase 2: ", e);
            throw e;  // ✅ Let ScraperOrchestrator handle the exception
        }

        return results;
    }


    /**
     * Filter tribunals that do NOT contain "Santiago" in the name
     */
    private List<String> filterOtherTribunals(Map<String, Integer> allTribunals) {
        List<String> others = new ArrayList<>();
        for (String name : allTribunals.keySet()) {
            if (!name.contains("Santiago") && !name.contains("Seleccione")) {
                others.add(name);
            }
        }
        return others;
    }

    @Override
    public String getPhaseName() {
        return "Phase 2: Other Tribunals (excluding Santiago)";
    }
}











