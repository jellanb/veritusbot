package com.example.veritusbot.service;

import com.example.veritusbot.dto.PersonaDTO;
import com.example.veritusbot.dto.ResultDTO;
import com.example.veritusbot.service.scraper.ScraperOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service responsible for asynchronous processing of search requests
 * Implements Single Responsibility Principle: only handles async processing
 */
@Service
public class AsyncProcessingService {
    private static final Logger logger = LoggerFactory.getLogger(AsyncProcessingService.class);

    private final ProcessingStateManager processingStateManager;
    private final ScraperOrchestrator scraperOrchestrator;
    private final DashboardStatusService dashboardStatusService;
    private final AtomicBoolean stopRequested = new AtomicBoolean(false);
    private volatile Thread currentProcessingThread;

    public AsyncProcessingService(ProcessingStateManager processingStateManager,
                                  ScraperOrchestrator scraperOrchestrator,
                                  DashboardStatusService dashboardStatusService) {
        this.processingStateManager = processingStateManager;
        this.scraperOrchestrator = scraperOrchestrator;
        this.dashboardStatusService = dashboardStatusService;
    }

    /**
     * Process search request asynchronously
     * Acquires lock before starting, releases after completion
     * 
     * @param people List of people to search
     * @param requestId Unique request identifier
     * @param isAllRegionEnabled true to include all tribunals (Phase 2), false to skip it
     * @param isSantiagoEnabled true to include Santiago tribunals (Phase 1), false to skip it
     */
    @Async
    public void processSearchAsync(
            List<PersonaDTO> people,
            String requestId,
            String searchName,
            boolean isAllRegionEnabled,
            boolean isSantiagoEnabled,
            int threadsPerPerson) {
        stopRequested.set(false);
        currentProcessingThread = Thread.currentThread();
        String firstPersonName = !people.isEmpty() ? people.get(0).getNombres() : "Unknown";

        // Try to acquire lock
        if (!processingStateManager.tryAcquireLock(firstPersonName)) {
            logger.warn("⚠️  System is busy, request {} rejected", requestId);
            return;
        }

        try {
            dashboardStatusService.beginSearch(
                    requestId,
                    searchName,
                    people.size(),
                    isSantiagoEnabled,
                    isAllRegionEnabled
            );

            logger.info("▶️  Starting async processing for request: {}", requestId);
            logger.info("📋 Processing {} people", people.size());
            logger.info("🧭 All-region search enabled: {}", isAllRegionEnabled);
            logger.info("🏛️ Santiago search enabled: {}", isSantiagoEnabled);
            logger.info("🧵 Threads per person (runtime): {}", threadsPerPerson);

            // Execute scraping in background
            List<ResultDTO> results = scraperOrchestrator.scrapePeople(
                    people,
                    isAllRegionEnabled,
                    isSantiagoEnabled,
                    requestId,
                    threadsPerPerson);

            dashboardStatusService.addFoundResults(results.size());

            logger.info("✅ Async processing completed for request: {}", requestId);
            logger.info("📊 Found {} results", results.size());

            if (stopRequested.get()) {
                logger.warn("⏹️  Processing was stopped by user request for request: {}", requestId);
            }

        } catch (Exception e) {
            logger.error("❌ Error during async processing of request {}: {}", 
                requestId, e.getMessage(), e);
        } finally {
            currentProcessingThread = null;
            stopRequested.set(false);
            dashboardStatusService.finishSearch();
            // Always release lock
            processingStateManager.releaseLock();
            logger.info("🔓 Processing lock released for request: {}", requestId);
        }
    }

    /**
     * Check if system is currently processing
     * 
     * @return true if processing, false if idle
     */
    public boolean isBusy() {
        return processingStateManager.isProcessing();
    }

    /**
     * Get current processing state
     * 
     * @return ProcessingState with detailed information
     */
    public ProcessingStateManager.ProcessingState getState() {
        return processingStateManager.getState();
    }

    /**
     * Solicita detener el procesamiento en curso.
     *
     * @return true si se envio la solicitud de detencion, false si no habia proceso activo
     */
    public boolean requestStop() {
        if (!isBusy()) {
            return false;
        }

        stopRequested.set(true);
        Thread processingThread = currentProcessingThread;
        if (processingThread != null) {
            processingThread.interrupt();
        }

        logger.warn("⏹️  Stop requested for current processing");
        return true;
    }

    public boolean isStopRequested() {
        return stopRequested.get();
    }
}

