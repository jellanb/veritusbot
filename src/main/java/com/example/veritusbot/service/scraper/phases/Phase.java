package com.example.veritusbot.service.scraper.phases;

import com.example.veritusbot.dto.PersonaDTO;
import com.example.veritusbot.service.scraper.TribunalTrackingContext;
import com.example.veritusbot.service.scraper.retry.RetryableScraperException;
import com.microsoft.playwright.Page;
import java.util.List;
import com.example.veritusbot.dto.ResultDTO;

/**
 * Interface for scraping phases.
 * Each phase implements a specific search strategy.
 *
 * The primary method is {@link #execute(Page, PersonaDTO, int, int, int, String, TribunalTrackingContext)},
 * which supports name-based tribunal resume for retries.
 * All other overloads are convenience defaults that delegate to the primary method.
 */
public interface Phase {

    /**
     * PRIMARY method — must be implemented by each Phase.
     * Executes the phase scraping logic, resuming from a specific tribunal.
     *
     * @param page                  Playwright page instance
     * @param person                person to search
     * @param startYear             start year for the search range
     * @param endYear               end year for the search range
     * @param startTribunalPosition zero-based tribunal position to resume from (fallback when name is null)
     * @param startTribunalName     exact name of the tribunal to resume from (preferred on retry); null for fresh start
     * @param trackingContext       context required to persist tribunal execution status; may be null
     * @return List of results found
     * @throws RetryableScraperException when a retryable error occurs (browser/network)
     */
    List<ResultDTO> execute(
            Page page,
            PersonaDTO person,
            int startYear,
            int endYear,
            int startTribunalPosition,
            String startTribunalName,
            TribunalTrackingContext trackingContext) throws RetryableScraperException;

    /**
     * Convenience overload: fresh start (position 0, no name hint, no tracking).
     */
    default List<ResultDTO> execute(Page page, PersonaDTO person, int startYear, int endYear)
            throws RetryableScraperException {
        return execute(page, person, startYear, endYear, 0, null, null);
    }

    /**
     * Convenience overload: resume from a position, no name hint, no tracking.
     */
    default List<ResultDTO> execute(Page page, PersonaDTO person, int startYear, int endYear, int startTribunalPosition)
            throws RetryableScraperException {
        return execute(page, person, startYear, endYear, startTribunalPosition, null, null);
    }

    /**
     * Convenience overload: resume from a position with tracking context, no name hint.
     */
    default List<ResultDTO> execute(
            Page page,
            PersonaDTO person,
            int startYear,
            int endYear,
            int startTribunalPosition,
            TribunalTrackingContext trackingContext) throws RetryableScraperException {
        return execute(page, person, startYear, endYear, startTribunalPosition, null, trackingContext);
    }

    /**
     * Get the name of this phase.
     *
     * @return Phase name
     */
    String getPhaseName();
}
