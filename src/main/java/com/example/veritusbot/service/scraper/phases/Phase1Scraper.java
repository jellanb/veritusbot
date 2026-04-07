package com.example.veritusbot.service.scraper.phases;

import com.example.veritusbot.dto.PersonaDTO;
import com.example.veritusbot.dto.ResultDTO;
import com.example.veritusbot.service.ResultPersistenceService;
import com.example.veritusbot.service.TribunalBusquedaService;
import com.example.veritusbot.service.scraper.browser.BrowserManager;
import com.example.veritusbot.service.scraper.browser.FrameNavigator;
import com.example.veritusbot.service.scraper.browser.HumanBehaviorService;
import com.example.veritusbot.service.scraper.form.FormFiller;
import com.example.veritusbot.service.scraper.form.TribunalSelector;
import com.example.veritusbot.service.scraper.parser.ResultParser;
import com.example.veritusbot.service.scraper.TribunalTrackingContext;
import com.microsoft.playwright.Frame;
import com.microsoft.playwright.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.*;
import com.example.veritusbot.service.scraper.retry.RetryableScraperException;

/**
 * Phase 1 Scraper: Search in Santiago tribunals (1º - 30º)
 * Persists results immediately when found (not at the end)
 */
@Component
public class Phase1Scraper implements Phase {
    private static final Logger logger = LoggerFactory.getLogger(Phase1Scraper.class);

    private final FormFiller formFiller;
    private final TribunalSelector tribunalSelector;
    private final ResultParser resultParser;
    private final FrameNavigator frameNavigator;
    private final ResultPersistenceService resultPersistenceService;
    private final HumanBehaviorService humanBehaviorService;
    private final BrowserManager browserManager;
    private final TribunalBusquedaService tribunalBusquedaService;

    public Phase1Scraper(FormFiller formFiller, TribunalSelector tribunalSelector,
                         ResultParser resultParser, FrameNavigator frameNavigator,
                         ResultPersistenceService resultPersistenceService,
                         HumanBehaviorService humanBehaviorService,
                         BrowserManager browserManager,
                         TribunalBusquedaService tribunalBusquedaService) {
        this.formFiller = formFiller;
        this.tribunalSelector = tribunalSelector;
        this.resultParser = resultParser;
        this.frameNavigator = frameNavigator;
        this.resultPersistenceService = resultPersistenceService;
        this.humanBehaviorService = humanBehaviorService;
        this.browserManager = browserManager;
        this.tribunalBusquedaService = tribunalBusquedaService;
    }

    /**
     * PRIMARY execute implementation.
     * On retry, {@code startTribunalName} is used to locate the failed tribunal by name in the
     * freshly loaded list (more robust than position-based resume). Falls back to
     * {@code startTribunalPosition} when the name is null or not found.
     */
    @Override
    public List<ResultDTO> execute(
            Page page,
            PersonaDTO person,
            int startYear,
            int endYear,
            int startTribunalPosition,
            String startTribunalName,
            TribunalTrackingContext trackingContext) throws RetryableScraperException {
        List<ResultDTO> results = new ArrayList<>();

        try {
            logger.info("▶️  Starting Phase 1 (Santiago tribunals 1-30)...");
            logger.debug("🧾 Phase 1 input person={} {} {}, year={} resumePos={} resumeName={} proxy={}",
                    person.getNombres(),
                    person.getApellidoPaterno(),
                    person.getApellidoMaterno(),
                    startYear,
                    startTribunalPosition,
                    startTribunalName,
                    browserManager.getProxyLabel(page));

            // Navigate to search form
            frameNavigator.navigateToSearchForm(page);

            // Get search frame
            Frame searchFrame = frameNavigator.getSearchFrame(page);
            if (searchFrame == null) {
                logger.debug("🪟 Search frame not found, using main frame");
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
            logger.debug("📚 Loaded {} total tribunals before filtering", allTribunals.size());

            tribunalSelector.closeDropdown(searchFrame);
            humanBehaviorService.pauseShort(page);

            // Filter to only Santiago tribunals (1-30)
            List<Map.Entry<String, Integer>> santiagoTribunals = filterSantiagoTribunals(allTribunals);
            registerTribunalsIfNeeded(santiagoTribunals, startYear, trackingContext);

            if (santiagoTribunals.isEmpty()) {
                logger.warn("⚠ No Santiago tribunals found");
                return results;
            }

            // Resolve resume position: name-based (on retry) is preferred over position-based
            int resumePosition = resolveResumePosition(santiagoTribunals, startTribunalPosition, startTribunalName);
            if (resumePosition >= santiagoTribunals.size()) {
                logger.warn("⚠ Resume position {} is out of range for {} tribunals", resumePosition, santiagoTribunals.size());
                return results;
            }

            logger.info("📋 Phase 1: Found {} Santiago tribunals (resume from position {}{})",
                    santiagoTribunals.size(),
                    resumePosition,
                    startTribunalName != null ? " [retry tribunal: '" + startTribunalName + "']" : "");

            // Search in each Santiago tribunal
            for (int tribunalPosition = resumePosition; tribunalPosition < santiagoTribunals.size(); tribunalPosition++) {
                Map.Entry<String, Integer> tribunalEntry = santiagoTribunals.get(tribunalPosition);
                String tribunalName = tribunalEntry.getKey();
                logger.debug("📍 Phase 1 tribunal checkpoint {}/{} -> {}",
                        tribunalPosition + 1,
                        santiagoTribunals.size(),
                        tribunalName);
                try {
                    Integer tribunalIndex = tribunalEntry.getValue();
                    if (tribunalIndex == null) {
                        markTribunalWithError(trackingContext, tribunalName, startYear,
                                "ERROR_SCRAPER: tribunal index is null");
                        logger.debug("⏭️  Skipping tribunal {} because index is null", tribunalName);
                        continue;
                    }

                    logger.debug("🔍 Searching in: {} (index: {}, proxy: {})",
                            tribunalName,
                            tribunalIndex,
                            browserManager.getProxyLabel(page));

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
                    } else {
                        logger.debug("ℹ️  No results in tribunal {} for year {}", tribunalName, startYear);
                    }

                    markTribunalCompleted(trackingContext, tribunalName, startYear);

                } catch (Exception e) {
                    // ✅ Throw exception with context for ScraperOrchestrator to handle retries
                    String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                    logger.error("❌ Error searching tribunal {}: {}", tribunalName, errorMsg);

                    // If it's a browser/network error, let ScraperOrchestrator know to retry
                    // Otherwise, log and continue to next tribunal
                    if (RetryableScraperException.isBrowserOrNetworkError(e)) {
                        markTribunalWithError(trackingContext, tribunalName, startYear,
                                "ERROR_CONEXION: " + errorMsg);
                        throw new RetryableScraperException(
                                "Browser/Network error while searching tribunal: " + tribunalName,
                                e,
                                true,           // isRetryable
                                "tribunal: " + tribunalName,
                                tribunalPosition,
                                tribunalName    // ✅ Include tribunal name for name-based retry
                        );
                    } else {
                        markTribunalWithError(trackingContext, tribunalName, startYear,
                                "ERROR_SCRAPER: " + errorMsg);
                        logger.warn("⚠ Non-retryable error in tribunal {}, continuing to next...", tribunalName);
                    }
                }
            }

            logger.info("✓ Phase 1 completed. Found {} results", results.size());

        } catch (Exception e) {
            logger.error("❌ Error in Phase 1: ", e);
            throw e;  // ✅ Let ScraperOrchestrator handle the exception
        }

        return results;
    }

    /**
     * Resolve the position to start (or resume) the tribunal loop from.
     *
     * On retry, {@code nameHint} is the exact name of the failed tribunal.
     * Searching by name is more robust than by position because the loaded list order
     * could theoretically differ between attempts.
     * Falls back to {@code positionHint} when the name is null or not found in the list.
     *
     * @param tribunals    filtered, sorted tribunal list for this phase
     * @param positionHint zero-based position hint (from {@code startTribunalPosition})
     * @param nameHint     exact tribunal name to locate (may be null on fresh start)
     * @return resolved zero-based position to start the loop from
     */
    private int resolveResumePosition(
            List<Map.Entry<String, Integer>> tribunals,
            int positionHint,
            String nameHint) {
        if (nameHint != null && !nameHint.isBlank()) {
            for (int i = 0; i < tribunals.size(); i++) {
                if (tribunals.get(i).getKey().equals(nameHint)) {
                    logger.info("🎯 [Retry] Tribunal '{}' resolved to position {} by name", nameHint, i);
                    return i;
                }
            }
            logger.warn("⚠ [Retry] Tribunal '{}' not found by name in loaded list, falling back to position {}",
                    nameHint, positionHint);
        }
        return Math.max(0, positionHint);
    }

    /**
     * Filter tribunals that contain "Santiago" in the name
     */
    private List<Map.Entry<String, Integer>> filterSantiagoTribunals(Map<String, Integer> allTribunals) {
        List<Map.Entry<String, Integer>> santiago = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : allTribunals.entrySet()) {
            String name = entry.getKey();
            if (name.contains("Santiago") && !name.contains("Seleccione")) {
                santiago.add(entry);
            }
        }
        santiago.sort(Comparator.comparingInt(entry -> entry.getValue() != null ? entry.getValue() : Integer.MAX_VALUE));
        logger.debug("🧮 Santiago tribunal filter reduced {} -> {} entries", allTribunals.size(), santiago.size());
        return santiago;
    }

    private void registerTribunalsIfNeeded(
            List<Map.Entry<String, Integer>> tribunals,
            int year,
            TribunalTrackingContext trackingContext) {
        if (trackingContext == null || tribunals.isEmpty()) {
            return;
        }

        List<String> tribunalNames = tribunals.stream()
                .map(Map.Entry::getKey)
                .toList();

        Map<String, Integer> tribunalIndexMap = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> tribunal : tribunals) {
            tribunalIndexMap.put(tribunal.getKey(), tribunal.getValue());
        }

        tribunalBusquedaService.registrarTribunalesAProcesar(
                trackingContext.personaProcesadaId(),
                trackingContext.requestId(),
                year,
                trackingContext.faseCodigo(),
                tribunalNames,
                tribunalIndexMap);
    }

    private void markTribunalCompleted(TribunalTrackingContext trackingContext, String tribunalName, int year) {
        if (trackingContext == null) {
            return;
        }

        tribunalBusquedaService.marcarTribunalCompletado(
                trackingContext.personaProcesadaId(),
                trackingContext.requestId(),
                tribunalName,
                trackingContext.faseCodigo(),
                year,
                true,
                null);
    }

    private void markTribunalWithError(
            TribunalTrackingContext trackingContext,
            String tribunalName,
            int year,
            String motivoError) {
        if (trackingContext == null) {
            return;
        }

        tribunalBusquedaService.marcarTribunalCompletado(
                trackingContext.personaProcesadaId(),
                trackingContext.requestId(),
                tribunalName,
                trackingContext.faseCodigo(),
                year,
                false,
                motivoError);
    }


    @Override
    public String getPhaseName() {
        return "Phase 1: Santiago Tribunals (1-30)";
    }
}














