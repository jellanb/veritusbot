package com.example.veritusbot.service.scraper;

import com.example.veritusbot.dto.PersonaDTO;
import com.example.veritusbot.dto.ResultDTO;
import com.example.veritusbot.model.PersonaProcesada;
import com.example.veritusbot.service.PersonProcessingService;
import com.example.veritusbot.service.PersonaProcesadaPersistenceService;
import com.example.veritusbot.service.ProcessingStateManager;
import com.example.veritusbot.service.ProxiSettingService;
import com.example.veritusbot.service.TribunalBusquedaService;
import com.example.veritusbot.service.scraper.browser.BrowserManager;
import com.example.veritusbot.service.scraper.phases.Phase;
import com.example.veritusbot.service.scraper.phases.Phase1Scraper;
import com.example.veritusbot.service.scraper.phases.Phase2Scraper;
import com.microsoft.playwright.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.ThreadLocalRandom;
import com.example.veritusbot.service.scraper.retry.RetryableScraperException;
import com.example.veritusbot.service.DashboardStatusService;

@Service
public class ScraperOrchestrator {
    private static final Logger logger = LoggerFactory.getLogger(ScraperOrchestrator.class);

    private final BrowserManager browserManager;
    private final Phase1Scraper phase1Scraper;
    private final Phase2Scraper phase2Scraper;
    private final PersonProcessingService personProcessingService;
    private final PersonaProcesadaPersistenceService personaProcesadaPersistenceService;
    private final TribunalBusquedaService tribunalBusquedaService;
    private final ProxiSettingService proxiSettingService;
    private final DashboardStatusService dashboardStatusService;

    @Value("${app.pjud.url}")
    private String pjudUrl;

    @Autowired
    public ScraperOrchestrator(BrowserManager browserManager,
                               Phase1Scraper phase1Scraper,
                               Phase2Scraper phase2Scraper,
                               PersonProcessingService personProcessingService,
                               ProcessingStateManager processingStateManager,
                               PersonaProcesadaPersistenceService personaProcesadaPersistenceService,
                               TribunalBusquedaService tribunalBusquedaService,
                               ProxiSettingService proxiSettingService,
                               DashboardStatusService dashboardStatusService) {
        this.browserManager = browserManager;
        this.phase1Scraper = phase1Scraper;
        this.phase2Scraper = phase2Scraper;
        this.personProcessingService = personProcessingService;
        this.personaProcesadaPersistenceService = personaProcesadaPersistenceService;
        this.tribunalBusquedaService = tribunalBusquedaService;
        this.proxiSettingService = proxiSettingService;
        this.dashboardStatusService = dashboardStatusService;

        // Reset state on initialization to prevent leftover state from previous runs
        processingStateManager.resetState();
    }

    // Constructor auxiliar para tests unitarios existentes
    ScraperOrchestrator(BrowserManager browserManager,
                        Phase1Scraper phase1Scraper,
                        Phase2Scraper phase2Scraper,
                        PersonProcessingService personProcessingService,
                        ProcessingStateManager processingStateManager,
                        PersonaProcesadaPersistenceService personaProcesadaPersistenceService,
                        TribunalBusquedaService tribunalBusquedaService) {
        this(browserManager,
                phase1Scraper,
                phase2Scraper,
                personProcessingService,
                processingStateManager,
                personaProcesadaPersistenceService,
                tribunalBusquedaService,
                null,
                null);
    }

    /**
     * Scrape data for a list of people
     * Process one person at a time with threading for their year range
     * Persist results immediately to CSV and database after each person
     * @param people List of PersonaDTO objects to scrape
     * @param isAllRegionEnabled true to process all tribunals (Phase 2), false to run only Phase 1
     * @param isSantiagoEnabled true to process Santiago tribunals (Phase 1), false to skip it
     * @param requestId Request ID for tracking
     * @return List of ResultDTO with all found results
     */
    public List<ResultDTO> scrapePeople(
            List<PersonaDTO> people,
            boolean isAllRegionEnabled,
            boolean isSantiagoEnabled,
            String requestId,
            int threadsPerPerson) {
        List<ResultDTO> allResults = new ArrayList<>();
        List<PersonaDTO> peopleToProcess = people != null ? people : Collections.emptyList();
        int effectiveMaxThreads = resolveEffectiveMaxThreads(threadsPerPerson);

        try {
            logger.info("🚀 Starting scraper orchestrator with max {} threads per client...", effectiveMaxThreads);
            logger.debug("📥 Received {} people to process", peopleToProcess.size());

            if (isSantiagoEnabled) {
                // Phase 1: Santiago tribunals
                List<PersonaDTO> phase1People = personProcessingService.filterPeopleForPhase1(peopleToProcess);
                logger.debug("🧭 Phase 1 candidate count: {}", phase1People.size());

                if (!phase1People.isEmpty()) {
                    logger.info("▶️  PHASE 1: Processing Santiago tribunals...");
                    for (PersonaDTO person : phase1People) {
                        PersonaProcesada personaProcesada = personaProcesadaPersistenceService.getOrCreatePersonaProcesada(person);
                        logger.debug("👤 [PHASE 1] Starting person {} {} {}", person.getNombres(), person.getApellidoPaterno(), person.getApellidoMaterno());
                        List<ResultDTO> personResults = processPersonWithThreadPool(
                                person,
                                phase1Scraper,
                                "PHASE 1",
                                "PHASE_1",
                                requestId,
                                personaProcesada.getId(),
                                effectiveMaxThreads);
                        allResults.addAll(personResults);
                        if (dashboardStatusService != null) {
                            dashboardStatusService.markSantiagoProgress();
                        }
                        logger.debug("📦 [PHASE 1] Person finished with {} results", personResults.size());

                        try {
                            if (tribunalBusquedaService.puedeMarcarComoProcessada(personaProcesada.getId(), requestId, "PHASE_1")) {
                                logger.info("📝 Marking person as processed after Phase 1: {} {} {}",
                                    person.getNombres(), person.getApellidoPaterno(), person.getApellidoMaterno());

                                personaProcesadaPersistenceService.markTribunalPrincipalAsProcessed(personaProcesada);

                                logger.info("✅ Person marked as processed in tracking table (tribunal_principal_procesado=true)");
                            } else {
                                logger.warn("⚠️ Person NOT marked after Phase 1 because some tribunals ended with scraper/connection errors or remained pending: {} {} {}",
                                    person.getNombres(), person.getApellidoPaterno(), person.getApellidoMaterno());
                            }
                        } catch (Exception e) {
                            logger.error("❌ Error evaluating person completion after Phase 1: {}", e.getMessage(), e);
                        }
                    }
                    logger.info("✅ Phase 1 completed. Found {} results", allResults.size());
                } else {
                    logger.info("ℹ️  Phase 1: No people pending");
                }
            } else {
                logger.info("⏭️  Phase 1 skipped because Santiago search is disabled");
            }

            if (isAllRegionEnabled) {
                // Phase 2: Other tribunals
                List<PersonaDTO> phase2People = personProcessingService.filterPeopleForPhase2(peopleToProcess);
                logger.debug("🧭 Phase 2 candidate count: {}", phase2People.size());

                if (!phase2People.isEmpty()) {
                    logger.info("▶️  PHASE 2: Processing other tribunals...");
                    for (PersonaDTO person : phase2People) {
                        PersonaProcesada personaProcesada = personaProcesadaPersistenceService.getOrCreatePersonaProcesada(person);
                        logger.debug("👤 [PHASE 2] Starting person {} {} {}", person.getNombres(), person.getApellidoPaterno(), person.getApellidoMaterno());
                        List<ResultDTO> personResults = processPersonWithThreadPool(
                                person,
                                phase2Scraper,
                                "PHASE 2",
                                "PHASE_2",
                                requestId,
                                personaProcesada.getId(),
                                effectiveMaxThreads);
                        allResults.addAll(personResults);
                        if (dashboardStatusService != null) {
                            dashboardStatusService.markRegionesProgress();
                        }
                        logger.debug("📦 [PHASE 2] Person finished with {} results", personResults.size());

                        // Mark person as fully processed only when Phase 2 is executed
                        try {
                            if (tribunalBusquedaService.puedeMarcarComoProcessada(personaProcesada.getId(), requestId, "PHASE_2")) {
                                logger.info("📝 Marking person as completely processed after Phase 2: {} {} {}",
                                    person.getNombres(), person.getApellidoPaterno(), person.getApellidoMaterno());

                                personaProcesadaPersistenceService.markAsProcessed(personaProcesada);

                                logger.info("✅ Person marked as completely processed in tracking table (procesado=true)");
                            } else {
                                logger.warn("⚠️ Person NOT marked as completely processed after Phase 2 because some tribunals ended with scraper/connection errors or remained pending: {} {} {}",
                                    person.getNombres(), person.getApellidoPaterno(), person.getApellidoMaterno());
                            }
                        } catch (Exception e) {
                            logger.error("❌ Error evaluating person completion after Phase 2: {}", e.getMessage(), e);
                        }
                    }
                    logger.info("✅ Phase 2 completed. Total results: {}", allResults.size());
                } else {
                    logger.info("ℹ️  Phase 2: No people pending");
                }
            } else {
                logger.info("⏭️  Phase 2 skipped because all-region search is disabled");
            }

        } catch (Exception e) {
            logger.error("❌ Fatal error in scraper: ", e);
        }

        logger.debug("🏁 Scraper orchestrator finished with {} aggregated results", allResults.size());

        return allResults;
    }

    /**
     * Process a single person with threading for their year range
     * Maximum threads are capped by active proxies in DB (1 proxy = 1 max thread)
     * @param person Person to process
     * @param phase Phase to execute
     * @param phaseName Phase name for logging
     * @return List of results for this person
     */
    private List<ResultDTO> processPersonWithThreadPool(
            PersonaDTO person,
            Phase phase,
            String phaseName,
            String phaseCode,
            String requestId,
            Integer personaProcesadaId,
            int effectiveMaxThreads) {
        List<ResultDTO> personResults = Collections.synchronizedList(new ArrayList<>());
        ExecutorService executor = Executors.newFixedThreadPool(effectiveMaxThreads);
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
            logger.info("   📅 Total years: {}, Max threads: {}", totalYears, effectiveMaxThreads);

            // Submit a task for each year
            for (int year = person.getAnioInit(); year <= person.getAnioFin(); year++) {
                final int currentYear = year;
                logger.debug("🧵 Scheduling {} year {} for {}", phaseName, currentYear, clientKey);
                
                Future<Void> future = executor.submit(() -> {
                    String threadName = String.format("Thread-%s-%d", person.getNombres(), currentYear);
                    Thread.currentThread().setName(threadName);
                    logger.debug("▶️  [{}] Worker started for year {}", threadName, currentYear);
                    
                    // ✅ RETRY LOGIC: Open new browser on each retry
                    int maxRetries = 5;
                    int retryCount = 0;
                    boolean success = false;
                    int resumeFromTribunalPosition = 0;
                    
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
                            List<ResultDTO> yearResults = phase.execute(
                                    page,
                                    person,
                                    currentYear,
                                    currentYear,
                                    resumeFromTribunalPosition,
                                    new TribunalTrackingContext(personaProcesadaId, requestId, phaseCode));
                            personResults.addAll(yearResults);
                            
                            logger.info("   ✅ [{}] Year {} completed. Found {} results",
                                Thread.currentThread().getName(),
                                currentYear,
                                yearResults.size());
                            
                            success = true;  // ✅ Mark as successful
                            
                        } catch (RetryableScraperException e) {
                            // ✅ Special handling for retryable errors

                            Integer failedPosition = e.getFailedTribunalPosition();
                            if (failedPosition != null && failedPosition >= 0) {
                                resumeFromTribunalPosition = Math.max(resumeFromTribunalPosition, failedPosition);
                                logger.debug("   📍 [{}] Updating resume checkpoint to tribunal position {}",
                                    Thread.currentThread().getName(),
                                    resumeFromTribunalPosition);
                            }

                            if (!e.isRetryable()) {
                                logger.error("   ❌ [{}] Year {} FAILED (non-retryable): {}. Skipping to next year.",
                                    Thread.currentThread().getName(),
                                    currentYear,
                                    e.getMessage());
                                break;
                            }

                            if (retryCount < maxRetries) {
                                // ✅ Browser/Network error - Retry with new browser
                                logger.warn("   ⚠️  [{}] Retryable error ({}). Retrying with new browser from tribunal position {}... (Attempt {}/{})",
                                    Thread.currentThread().getName(),
                                    e.getContext(),
                                    resumeFromTribunalPosition,
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
                                // ❌ Max retries reached
                                logger.error("   ❌ [{}] Year {} FAILED (retries exceeded): {} (Attempt {}/{}). Skipping to next year.",
                                    Thread.currentThread().getName(),
                                    currentYear,
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
                            logger.debug("   🧪 [{}] Unexpected error class: {}", Thread.currentThread().getName(), e.getClass().getName());
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
                        logger.error("   ❌ [{}] Year {} could not be completed after {} attempt(s).",
                            Thread.currentThread().getName(),
                            currentYear,
                            retryCount);
                    } else {
                        logger.debug("   🏁 [{}] Year {} finished successfully", Thread.currentThread().getName(), currentYear);
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
                    logger.debug("   ✅ One year task finished for {}", clientKey);
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
            logger.debug("   📴 Shutting down executor for {}", buildClientKey(person));
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

    private int resolveEffectiveMaxThreads(int requestedThreadsPerPerson) {
        if (proxiSettingService == null) {
            return Math.max(1, requestedThreadsPerPerson);
        }

        if (requestedThreadsPerPerson < 1) {
            throw new IllegalArgumentException("threadsPerPerson debe ser mayor o igual a 1");
        }

        int activeProxyCount = proxiSettingService.listarActivos().size();
        if (activeProxyCount <= 0) {
            throw new IllegalStateException("No hay proxies activos en DB. Configura al menos 1 proxy activo para ejecutar búsquedas con hilos.");
        }

        if (requestedThreadsPerPerson > activeProxyCount) {
            logger.warn("⚠️ threadsPerPerson={} excede proxies activos={}. Se aplicará el límite de {} hilo(s).",
                    requestedThreadsPerPerson,
                    activeProxyCount,
                    activeProxyCount);
        }

        return Math.min(requestedThreadsPerPerson, activeProxyCount);
    }

    private String normalizeSegment(String value) {
        if (value == null || value.isBlank()) {
            return "na";
        }
        return value.toLowerCase().replaceAll("[^a-z0-9]", "_");
    }
}
