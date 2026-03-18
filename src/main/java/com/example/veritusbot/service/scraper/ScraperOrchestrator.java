package com.example.veritusbot.service.scraper;

import com.example.veritusbot.dto.PersonaDTO;
import com.example.veritusbot.dto.ResultDTO;
import com.example.veritusbot.service.PersonProcessingService;
import com.example.veritusbot.service.ProcessingStateManager;
import com.example.veritusbot.service.ResultPersistenceService;
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

@Service
public class ScraperOrchestrator {
    private static final Logger logger = LoggerFactory.getLogger(ScraperOrchestrator.class);

    @Value("${app.scraper.max-threads:3}")
    private int maxThreads;

    private final BrowserManager browserManager;
    private final Phase1Scraper phase1Scraper;
    private final Phase2Scraper phase2Scraper;
    private final PersonProcessingService personProcessingService;
    private final ResultPersistenceService resultPersistenceService;

    @Value("${app.pjud.url}")
    private String pjudUrl;

    public ScraperOrchestrator(BrowserManager browserManager,
                               Phase1Scraper phase1Scraper,
                               Phase2Scraper phase2Scraper,
                               PersonProcessingService personProcessingService,
                               ProcessingStateManager processingStateManager,
                               ResultPersistenceService resultPersistenceService) {
        this.browserManager = browserManager;
        this.phase1Scraper = phase1Scraper;
        this.phase2Scraper = phase2Scraper;
        this.personProcessingService = personProcessingService;
        this.resultPersistenceService = resultPersistenceService;

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
                    
                    // Persist results immediately after processing person
                    if (!personResults.isEmpty()) {
                        logger.info("💾 Persisting {} results for person: {} {} {}",
                            personResults.size(),
                            person.getNombres(),
                            person.getApellidoPaterno(),
                            person.getApellidoMaterno());
                        resultPersistenceService.saveResults(personResults, person);
                    }
                }
                personProcessingService.markPhase1Complete(phase1People);
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
                    
                    // Persist results immediately after processing person
                    if (!personResults.isEmpty()) {
                        logger.info("💾 Persisting {} results for person: {} {} {}",
                            personResults.size(),
                            person.getNombres(),
                            person.getApellidoPaterno(),
                            person.getApellidoMaterno());
                        resultPersistenceService.saveResults(personResults, person);
                    }
                }
                personProcessingService.markPhase2Complete(phase2People);
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
            logger.info("   📅 Total years: {}, Max threads: {}", totalYears, maxThreads);

            // Submit a task for each year
            for (int year = person.getAnioInit(); year <= person.getAnioFin(); year++) {
                final int currentYear = year;
                
                Future<Void> future = executor.submit(() -> {
                    String threadName = String.format("Thread-%s-%d", person.getNombres(), currentYear);
                    Thread.currentThread().setName(threadName);
                    
                    try {
                        logger.info("   🔄 [{}] Starting year {}", Thread.currentThread().getName(), currentYear);
                        
                        // Launch browser for this year
                        Page page = browserManager.launchBrowser();
                        
                        try {
                            browserManager.navigateTo(page, pjudUrl);
                            
                            // Execute phase for this year
                            List<ResultDTO> yearResults = phase.execute(page, person, currentYear, currentYear);
                            personResults.addAll(yearResults);
                            
                            logger.info("   ✅ [{}] Year {} completed. Found {} results",
                                Thread.currentThread().getName(),
                                currentYear,
                                yearResults.size());
                        } finally {
                            // Always close the browser
                            browserManager.closeBrowser(page);
                        }
                        
                    } catch (Exception e) {
                        logger.error("   ❌ [{}] Error processing year {}: {}",
                            Thread.currentThread().getName(),
                            currentYear,
                            e.getMessage(), e);
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
                    future.cancel(true);  // Cancel the task
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
}


