package com.example.veritusbot.service.scraper.phases;

import com.example.veritusbot.dto.PersonaDTO;
import com.microsoft.playwright.Page;
import java.util.List;
import com.example.veritusbot.dto.ResultDTO;

/**
 * Interface for scraping phases
 * Each phase implements a specific search strategy
 */
public interface Phase {
    /**
     * Execute the phase scraping logic
     * @param page Playwright page instance
     * @param person person to search
     * @param startYear Start year for the search range
     * @param endYear End year for the search range
     * @return List of results found
     */
    List<ResultDTO> execute(Page page, PersonaDTO person, int startYear, int endYear);

    /**
     * Get the name of this phase
     * @return Phase name
     */
    String getPhaseName();
}

