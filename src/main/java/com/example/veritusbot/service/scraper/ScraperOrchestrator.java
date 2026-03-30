package com.example.veritusbot.service.scraper;

import com.example.veritusbot.dto.PersonaDTO;
import com.example.veritusbot.dto.ResultDTO;
import com.example.veritusbot.service.PersonProcessingService;
import com.example.veritusbot.service.PersonaProcesadaPersistenceService;
import com.example.veritusbot.service.ProcessingStateManager;
import com.example.veritusbot.service.scraper.browser.BrowserManager;
import com.example.veritusbot.service.scraper.phases.Phase;
import com.example.veritusbot.service.scraper.phases.Phase1Scraper;
import com.example.veritusbot.service.scraper.phases.Phase2Scraper;
import com.microsoft.playwright.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.ThreadLocalRandom;
import com.example.veritusbot.service.scraper.retry.RetryableScraperException;

@Service
public class ScraperOrchestrator {
    private static final Logger logger = LoggerFactory.getLogger(ScraperOrchestrator.class);

    @Value("${app.scraper.max-threads:2}")
    private int maxThreads;

    private final BrowserManager browserManager;
    private final Phase1Scraper phase1Scraper;
    private final Phase2Scraper phase2Scraper;
    private final PersonProcessingService personProcessingService;
    private final PersonaProcesadaPersistenceService personaProcesadaPersistenceService;

    @Value("${app.pjud.url}")
    private String pjudUrl;

    public ScraperOrchestrator(BrowserManager browserManager,
                               Phase1Scraper phase1Scraper,
                               Phase2Scraper phase2Scraper,
                               PersonProcessingService personProcessingService,
                               ProcessingStateManager processingStateManager,
                               PersonaProcesadaPersistenceService personaProcesadaPersistenceService) {
        this.browserManager = browserManager;
        this.phase1Scraper = phase1Scraper;
        this.phase2Scraper = phase2Scraper;
        this.personProcessingService = personProcessingService;
        this.personaProcesadaPersistenceService = personaProcesadaPersistenceService;

        // Reset state on initialization to prevent leftover state from previous runs
        processingStateManager.resetState();
    }

    /**
     * Scrape data for a list of people
     * Process one person at a time with threading for their year range
     * Persist results immediately to CSV and database after each person
     * @param people List of PersonaDTO objects to scrape
     * @return List of ResultDTO with all found results
     */
    public List<ResultDTO> scrapePeople(List<PersonaDTO> people) {
        List<ResultDTO> allResults = new ArrayList<>();

        try {
            logger.info("🚀 Starting scraper orchestrator with max {} threads per client...", maxThreads);

            // Phase 1: Santiago tribunals
            List<PersonaDTO> phase1People = personProcessingService.filterPeopleForPhase1(people);
            
            if (!phase1People.isEmpty()) {
                logger.info("▶️  PHASE 1: Processing Santiago tribunals...");
                for (PersonaDTO person : phase1People) {
                    List<ResultDTO> personResults = processPersonWithThreadPool(person, phase1Scraper, "PHASE 1");
                    allResults.addAll(personResults);

                    try {
                        logger.info("📝 Marking person as processed after Phase 1: {} {} {}",
                            person.getNombres(), person.getApellidoPaterno(), person.getApellidoMaterno());
                        
                        personaProcesadaPersistenceService.markTribunalPrincipalAsProcessed(
                            personaProcesadaPersistenceService.getOrCreatePersonaProcesada(person));
                        
                        logger.info("✅ Person marked as processed in tracking table (tribunal_principal_procesado=true)");
                    } catch (Exception e) {
                        logger.error("❌ Error marking person as processed: {}", e.getMessage());
                    }
                }
                logger.info("✅ Phase 1 completed. Found {} results", allResults.size());
            } else {
                logger.info("ℹ️  Phase 1: No people pending");
            }

            // Phase 2: Other tribunals
            List<PersonaDTO> phase2People = personProcessingService.filterPeopleForPhase2(people);
            
            if (!phase2People.isEmpty()) {
                logger.info("▶️  PHASE 2: Processing other tribunals...");
                for (PersonaDTO person : phase2People) {
                    List<ResultDTO> personResults = processPersonWithThreadPool(person, phase2Scraper, "PHASE 2");
                    allResults.addAll(personResults);
                    
                    // ✅ MARK PERSON AS PROCESSED IMMEDIATELY (within the person loop)
                    try {
                        logger.info("📝 Marking person as completely processed after Phase 2: {} {} {}",
                            person.getNombres(), person.getApellidoPaterno(), person.getApellidoMaterno());
                        
                        personaProcesadaPersistenceService.markAsProcessed(
                            personaProcesadaPersistenceService.getOrCreatePersonaProcesada(person));
                        
                        logger.info("✅ Person marked as completely processed in tracking table (procesado=true)");
                    } catch (Exception e) {
                        logger.error("❌ Error marking person as processed: {}", e.getMessage());
                    }
                }
                logger.info("✅ Phase 2 completed. Total results: {}", allResults.size());
            } else {
                logger.info("ℹ️  Phase 2: No people pending");
            }

        } catch (Exception e) {
            logger.error("❌ Fatal error in scraper: ", e);
        }

        return allResults;
    }

    /**
     * Process a single person with threading for their year range
     * Maximum 3 threads (browsers) for the year range
     * @param person Person to process
     * @param phase Phase to execute
     * @param phaseName Phase name for logging
     * @return List of results for this person
     */
    private List<ResultDTO> processPersonWithThreadPool(PersonaDTO person, Phase phase, String phaseName) {
        List<ResultDTO> personResults = Collections.synchronizedList(new ArrayList<>());
        ExecutorService executor = Executors.newFixedThreadPool(maxThreads);
        List<Future<Void>> futures = new ArrayList<>();

        try {
            logger.info("🔍 Processing: {} {} {} (Years: {}-{}) [{}]",
                person.getNombres(),
                person.getApellidoPaterno(),
                person.getApellidoMaterno(),
                person.getAnioInit(),
                person.getAnioFin(),
                phaseName);

            int totalYears = person.getAnioFin() - person.getAnioInit() + 1;
            String clientKey = buildClientKey(person);
            logger.info("   📅 Total years: {}, Max threads: {}", totalYears, maxThreads);

            // Submit a task for each year
            for (int year = person.getAnioInit(); year <= person.getAnioFin(); year++) {
                final int currentYear = year;
                
                Future<Void> future = executor.submit(() -> {
                    String threadName = String.format("Thread-%s-%d", person.getNombres(), currentYear);
                    Thread.currentThread().setName(threadName);
                    
                    // ✅ RETRY LOGIC: Open new browser on each retry
                    int maxRetries = 5;
                    int retryCount = 0;
                    boolean success = false;
                    
                    while (retryCount < maxRetries && !success) {
                        retryCount++;
                        Page page = null;
                        
                        try {
                            logger.info("   🔄 [{}] Starting year {} (Attempt {}/{})", 
                                Thread.currentThread().getName(), 
                                currentYear,
                                retryCount,
                                maxRetries);
                            
                            // ✅ Create FRESH browser for each attempt
                            page = browserManager.launchBrowser(clientKey);
                            browserManager.navigateTo(page, pjudUrl);
                            
                            // ✅ Execute phase for this year
                            // NOTE: Phase1Scraper/Phase2Scraper throw RetryableScraperException on errors
                            List<ResultDTO> yearResults = phase.execute(page, person, currentYear, currentYear);
                            personResults.addAll(yearResults);
                            
                            logger.info("   ✅ [{}] Year {} completed. Found {} results",
                                Thread.currentThread().getName(),
                                currentYear,
                                yearResults.size());
                            
                            success = true;  // ✅ Mark as successful
                            
                        } catch (RetryableScraperException e) {
                            // ✅ Special handling for retryable errors
                            
                            if (e.isRetryable() && retryCount < maxRetries) {
                                // ✅ Browser/Network error - Retry with new browser
                                logger.warn("   ⚠️  [{}] Retryable error ({}). Retrying with new browser... (Attempt {}/{})",
                                    Thread.currentThread().getName(),
                                    e.getContext(),
                                    retryCount,
                                    maxRetries);
                                
                                // Exponential backoff: 1s, 2s, 4s, 8s, 16s
                                long baseBackoffMs = (long) Math.pow(2, retryCount - 1) * 1000;
                                long jitterMs = ThreadLocalRandom.current().nextLong(150, 651);
                                long backoffMs = baseBackoffMs + jitterMs;
                                try {
                                    logger.debug("   ⏳ [{}] Waiting {}ms before retry attempt {}...", 
                                        Thread.currentThread().getName(), backoffMs, retryCount + 1);
                                    Thread.sleep(backoffMs);
                                } catch (InterruptedException ie) {
                                    Thread.currentThread().interrupt();
                                    logger.warn("   ⚠️  [{}] Retry wait interrupted", Thread.currentThread().getName());
                                    break;
                                }
                                // ↻ Loop continues, opens NEW browser
                                
                            } else {
                                // ❌ Non-retryable error or max retries reached
                                logger.error("   ❌ [{}] Year {} FAILED ({}): {} (Attempt {}/{}). Skipping to next year.",
                                    Thread.currentThread().getName(),
                                    currentYear,
                                    e.isRetryable() ? "retries exceeded" : "non-retryable",
                                    e.getMessage(),
                                    retryCount,
                                    maxRetries);
                            }
                            
                        } catch (Exception e) {
                            // ❌ Unexpected error - Log and continue
                            
                            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                            logger.error("   ❌ [{}] Unexpected error in year {}: {} (Attempt {}/{}). Skipping to next year.",
                                Thread.currentThread().getName(),
                                currentYear,
                                errorMsg,
                                retryCount,
                                maxRetries);
                        } finally {
                            // Always close the browser for this attempt (success or failure)
                            if (page != null) {
                                try {
                                    browserManager.closeBrowser(page);
                                } catch (Exception closingException) {
                                    logger.debug("   ℹ️  Browser already closed or error while closing");
                                }
                            }
                        }
                    }
                    
                    if (!success) {
                        logger.error("   ❌ [{}] Year {} could not be completed after {} attempts.",
                            Thread.currentThread().getName(),
                            currentYear,
                            maxRetries);
                    }
                    
                    return null;
                });
                
                futures.add(future);
            }


            // Wait for all year tasks to complete
            logger.info("   ⏳ Waiting for {} year(s) to complete...", futures.size());
            
            for (Future<Void> future : futures) {
                try {
                    future.get(20, TimeUnit.MINUTES);
                } catch (TimeoutException e) {
                    logger.warn("   ⚠️  Year processing timeout - possible site issue");
                    future.cancel(true);
                } catch (ExecutionException e) {
                    logger.error("   ❌ Year processing failed: {}", e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
                } catch (InterruptedException e) {
                    logger.warn("   ⚠️  Year processing interrupted");
                    Thread.currentThread().interrupt();
                }
            }

            logger.info("   ✓ Person {} completed with {} results",
                person.getNombres(),
                personResults.size());

        } finally {
            // Shutdown executor PROPERLY
            executor.shutdown();
            try {
                if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                    logger.warn("   ⚠️  Executor did not terminate, forcing shutdown");
                    List<Runnable> remaining = executor.shutdownNow();
                    if (!remaining.isEmpty()) {
                        logger.warn("   ⚠️  {} tasks were cancelled", remaining.size());
                    }
                }
            } catch (InterruptedException e) {
                logger.warn("   ⚠️  Interrupted while waiting for executor termination");
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        return new ArrayList<>(personResults);
    }

    private String buildClientKey(PersonaDTO person) {
        return String.join("-",
                normalizeSegment(person.getNombres()),
                normalizeSegment(person.getApellidoPaterno()),
                normalizeSegment(person.getApellidoMaterno()));
    }

    private String normalizeSegment(String value) {
        if (value == null || value.isBlank()) {
            return "na";
        }
        return value.toLowerCase().replaceAll("[^a-z0-9]", "_");
    }
}
