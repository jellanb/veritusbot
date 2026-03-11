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
 * Phase 2 Scraper: Search in other tribunals (excluding Santiago)
 */
@Component
public class Phase2Scraper implements Phase {
    private static final Logger logger = LoggerFactory.getLogger(Phase2Scraper.class);

    private final FormFiller formFiller;
    private final TribunalSelector tribunalSelector;
    private final ResultParser resultParser;

    public Phase2Scraper(FormFiller formFiller, TribunalSelector tribunalSelector, ResultParser resultParser) {
        this.formFiller = formFiller;
        this.tribunalSelector = tribunalSelector;
        this.resultParser = resultParser;
    }

    @Override
    public List<ResultDTO> execute(Page page, String personName, int startYear, int endYear) {
        List<ResultDTO> results = new ArrayList<>();

        try {
            logger.info("▶️  Starting Phase 2 (Other tribunals)...");
            // Implementation will be moved from PjudScraper
            // Focus on filtering tribunals WITHOUT "Santiago" in the name

        } catch (Exception e) {
            logger.error("❌ Error in Phase 2: ", e);
        }

        return results;
    }

    @Override
    public String getPhaseName() {
        return "Phase 2: Other Tribunals";
    }
}

