package com.example.veritusbot.service.scraper;

import com.example.veritusbot.dto.PersonaDTO;
import com.example.veritusbot.dto.ResultDTO;
import com.example.veritusbot.service.scraper.browser.BrowserManager;
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
        Page page = null;

        try {
            logger.info("🚀 Starting scraper orchestrator...");
            page = browserManager.launchBrowser();
            browserManager.navigateTo(page, "https://oficinajudicialvirtual.pjud.cl/home/index.php");

            int counter = 1;
            for (PersonaDTO person : people) {
                logger.info("🔍 [{}/{}] Processing: {} {} {}",
                    counter,
                    people.size(),
                    person.getNombres(),
                    person.getApellidoPaterno(),
                    person.getApellidoMaterno());

                List<ResultDTO> personResults = new ArrayList<>();

                try {
                    // Execute Phase 1 (search by year - Santiago tribunals)
                    logger.debug("📋 Executing Phase 1 (Santiago tribunals)...");
                    List<ResultDTO> phase1Results = phase1Scraper.execute(page, person.getNombres(),
                                                                          person.getAnoInit(),
                                                                          person.getAnoFin());
                    personResults.addAll(phase1Results);

                    // Execute Phase 2 (search by tribunal - Other tribunals)
                    logger.debug("📋 Executing Phase 2 (Other tribunals)...");
                    List<ResultDTO> phase2Results = phase2Scraper.execute(page, person.getNombres(),
                                                                          person.getAnoInit(),
                                                                          person.getAnoFin());
                    personResults.addAll(phase2Results);

                    allResults.addAll(personResults);
                    logger.info("✅ Person processed: {} results found", personResults.size());

                } catch (Exception e) {
                    logger.error("❌ Error processing person: {} {} - {}",
                        person.getNombres(),
                        person.getApellidoPaterno(),
                        e.getMessage());
                }

                counter++;
            }

            logger.info("✅ Scraping completed. Total results: {}", allResults.size());

        } catch (Exception e) {
            logger.error("❌ Fatal error in scraper: ", e);
        } finally {
            browserManager.closeBrowser();
        }

        return allResults;
    }
}

