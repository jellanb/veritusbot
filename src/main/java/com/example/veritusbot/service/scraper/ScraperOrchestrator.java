package com.example.veritusbot.service.scraper;

import com.example.veritusbot.dto.PersonaDTO;
import com.example.veritusbot.dto.ResultDTO;
import com.example.veritusbot.service.scraper.browser.BrowserManager;
import com.example.veritusbot.service.scraper.phases.Phase;
import com.example.veritusbot.service.scraper.phases.Phase1Scraper;
import com.example.veritusbot.service.scraper.phases.Phase2Scraper;
import com.microsoft.playwright.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class ScraperOrchestrator {
    private static final Logger logger = LoggerFactory.getLogger(ScraperOrchestrator.class);

    private final BrowserManager browserManager;
    private final Phase1Scraper phase1Scraper;
    private final Phase2Scraper phase2Scraper;

    public ScraperOrchestrator(BrowserManager browserManager,
                               Phase1Scraper phase1Scraper,
                               Phase2Scraper phase2Scraper) {
        this.browserManager = browserManager;
        this.phase1Scraper = phase1Scraper;
        this.phase2Scraper = phase2Scraper;
    }

    /**
     * Scrape data for a list of people
     * @param people List of PersonaDTO objects to scrape
     * @return List of ResultDTO with all found results
     */
    public List<ResultDTO> scrapePeople(List<PersonaDTO> people) {
        List<ResultDTO> allResults = new ArrayList<>();
        Page page;

        try {
            logger.info("🚀 Starting scraper orchestrator...");
            page = browserManager.launchBrowser();
            browserManager.navigateTo(page, "https://oficinajudicialvirtual.pjud.cl/home/index.php");

            // Execute Phase 1 for all people
            logger.info("▶️  PHASE 1: Processing Santiago tribunals...");
            List<ResultDTO> phase1Results = executePhaseForAllPeople(page, people, phase1Scraper);
            allResults.addAll(phase1Results);

            // Execute Phase 2 for all people
            logger.info("▶️  PHASE 2: Processing other tribunals...");
            List<ResultDTO> phase2Results = executePhaseForAllPeople(page, people, phase2Scraper);
            allResults.addAll(phase2Results);

            logger.info("✅ Scraping completed. Total results: {}", allResults.size());

        } catch (Exception e) {
            logger.error("❌ Fatal error in scraper: ", e);
        } finally {
            browserManager.closeBrowser();
        }

        return allResults;
    }

    /**
     * Execute a specific phase for all people in the list
     * @param page Playwright page instance
     * @param people List of people to process
     * @param phase Phase implementation to execute
     * @return List of results from this phase
     */
    private List<ResultDTO> executePhaseForAllPeople(Page page, List<PersonaDTO> people, Phase phase) {
        List<ResultDTO> phaseResults = new ArrayList<>();

        logger.info("📋 Executing phase: {}", phase.getPhaseName());

        int counter = 1;
        for (PersonaDTO person : people) {
            logger.info("🔍 [{}/{}] Processing: {} {} {}",
                counter,
                people.size(),
                person.getNombres(),
                person.getApellidoPaterno(),
                person.getApellidoMaterno());

            try {
                // Execute phase for this person
                List<ResultDTO> personResults = phase.execute(page, person.getNombres(),
                                                              person.getAnioInit(),
                                                              person.getAnioFin());
                phaseResults.addAll(personResults);

                logger.info("✅ Person processed: {} results found", personResults.size());

            } catch (Exception e) {
                logger.error("❌ Error processing person: {} {} - {}",
                    person.getNombres(),
                    person.getApellidoPaterno(),
                    e.getMessage());
            }

            counter++;
        }

        logger.info("✓ {} completed. Found {} results", phase.getPhaseName(), phaseResults.size());
        return phaseResults;
    }
}


