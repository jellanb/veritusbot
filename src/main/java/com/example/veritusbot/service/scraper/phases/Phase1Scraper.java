package com.example.veritusbot.service.scraper.phases;

import com.example.veritusbot.dto.PersonaDTO;
import com.example.veritusbot.dto.ResultDTO;
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
 */
@Component
public class Phase1Scraper implements Phase {
    private static final Logger logger = LoggerFactory.getLogger(Phase1Scraper.class);

    private final FormFiller formFiller;
    private final TribunalSelector tribunalSelector;
    private final ResultParser resultParser;
    private final FrameNavigator frameNavigator;

    public Phase1Scraper(FormFiller formFiller, TribunalSelector tribunalSelector,
                         ResultParser resultParser, FrameNavigator frameNavigator) {
        this.formFiller = formFiller;
        this.tribunalSelector = tribunalSelector;
        this.resultParser = resultParser;
        this.frameNavigator = frameNavigator;
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

                    logger.debug("🔍 Searching in: {} (index: {})", tribunalName, tribunalIndex);

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
                    }

                } catch (Exception e) {
                    logger.warn("⚠ Error searching tribunal {}: {}", tribunalName, e.getMessage());
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



