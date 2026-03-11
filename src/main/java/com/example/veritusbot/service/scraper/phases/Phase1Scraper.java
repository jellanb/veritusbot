package com.example.veritusbot.service.scraper.phases;

import com.example.veritusbot.dto.ResultDTO;
import com.example.veritusbot.service.scraper.form.FormFiller;
import com.example.veritusbot.service.scraper.form.TribunalSelector;
import com.example.veritusbot.service.scraper.parser.ResultParser;
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

    public Phase1Scraper(FormFiller formFiller, TribunalSelector tribunalSelector, ResultParser resultParser) {
        this.formFiller = formFiller;
        this.tribunalSelector = tribunalSelector;
        this.resultParser = resultParser;
    }

    @Override
    public List<ResultDTO> execute(Page page, String personName, int startYear, int endYear) {
        List<ResultDTO> results = new ArrayList<>();

        try {
            logger.info("▶️  Starting Phase 1 (Santiago tribunals)...");
            // Implementation will be moved from PjudScraper
            // Focus on filtering tribunals with "Santiago" in the name

        } catch (Exception e) {
            logger.error("❌ Error in Phase 1: ", e);
        }

        return results;
    }

    @Override
    public String getPhaseName() {
        return "Phase 1: Santiago Tribunals";
    }
}

