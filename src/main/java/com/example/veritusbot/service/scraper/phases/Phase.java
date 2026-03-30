package com.example.veritusbot.service.scraper.phases;

import com.example.veritusbot.dto.PersonaDTO;
import com.example.veritusbot.service.scraper.retry.RetryableScraperException;
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
     * @throws RetryableScraperException when a retryable error occurs (browser/network)
     */
    default List<ResultDTO> execute(Page page, PersonaDTO person, int startYear, int endYear)
            throws RetryableScraperException {
        return execute(page, person, startYear, endYear, 0);
    }

    /**
     * Execute the phase scraping logic resuming from a tribunal position.
     *
     * @param page Playwright page instance
     * @param person person to search
     * @param startYear Start year for the search range
     * @param endYear End year for the search range
     * @param startTribunalPosition zero-based tribunal position to resume from
     * @return List of results found
     * @throws RetryableScraperException when a retryable error occurs (browser/network)
     */
    List<ResultDTO> execute(Page page, PersonaDTO person, int startYear, int endYear, int startTribunalPosition)
            throws RetryableScraperException;

    /**
     * Get the name of this phase
     * @return Phase name
     */
    String getPhaseName();
}

