package com.example.veritusbot.service.scraper;

import com.example.veritusbot.dto.PersonaDTO;
import com.example.veritusbot.dto.ResultDTO;
import com.example.veritusbot.service.scraper.browser.BrowserManager;
import com.microsoft.playwright.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class ScraperOrchestrator {
    private static final Logger logger = LoggerFactory.getLogger(ScraperOrchestrator.class);

    private final BrowserManager browserManager;
    // Phase1Scraper and Phase2Scraper will be implemented after
    // For now, this is the structure

    public ScraperOrchestrator(BrowserManager browserManager) {
        this.browserManager = browserManager;
    }

    /**
     * Scrape data for a list of people
     * @param people List of PersonaDTO objects to scrape
     * @return List of ResultDTO with all found results
     */
    public List<ResultDTO> scrapePeople(List<PersonaDTO> people) {
        List<ResultDTO> allResults = new ArrayList<>();
        Page page = null;

        try {
            logger.info("🚀 Starting scraper orchestrator...");
            page = browserManager.launchBrowser();
            browserManager.navigateTo(page, "https://oficinajudicialvirtual.pjud.cl/home/index.php");

            for (PersonaDTO person : people) {
                logger.info("🔍 Processing: {} {} {}",
                    person.getNombres(),
                    person.getApellidoPaterno(),
                    person.getApellidoMaterno());

                List<ResultDTO> personResults = new ArrayList<>();

                try {
                    // Execute Phase 1 (search by year - Santiago tribunals)
                    logger.debug("📋 Executing Phase 1 (Santiago tribunals)...");
                    personResults.addAll(
                        executePhase1(page, person)
                    );

                    // Execute Phase 2 (search by tribunal - Other tribunals)
                    logger.debug("📋 Executing Phase 2 (Other tribunals)...");
                    personResults.addAll(
                        executePhase2(page, person)
                    );

                    allResults.addAll(personResults);
                    logger.info("✅ Person processed: {} results found", personResults.size());

                } catch (Exception e) {
                    logger.error("❌ Error processing person: {} {}",
                        person.getNombres(), e.getMessage());
                }
            }

            logger.info("✅ Scraping completed. Total results: {}", allResults.size());

        } catch (Exception e) {
            logger.error("❌ Fatal error in scraper: ", e);
        } finally {
            browserManager.closeBrowser();
        }

        return allResults;
    }

    /**
     * Execute Phase 1: Search in Santiago tribunals (1-30)
     * @param page Playwright page
     * @param person Person to search
     * @return List of results from Phase 1
     */
    private List<ResultDTO> executePhase1(Page page, PersonaDTO person) {
        // Will be implemented in Phase1Scraper
        return new ArrayList<>();
    }

    /**
     * Execute Phase 2: Search in other tribunals (31+)
     * @param page Playwright page
     * @param person Person to search
     * @return List of results from Phase 2
     */
    private List<ResultDTO> executePhase2(Page page, PersonaDTO person) {
        // Will be implemented in Phase2Scraper
        return new ArrayList<>();
    }
}

