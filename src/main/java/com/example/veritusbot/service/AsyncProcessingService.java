package com.example.veritusbot.service;

import com.example.veritusbot.dto.PersonaDTO;
import com.example.veritusbot.dto.ResultDTO;
import com.example.veritusbot.service.scraper.ScraperOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * Service responsible for asynchronous processing of search requests
 * Implements Single Responsibility Principle: only handles async processing
 */
@Service
public class AsyncProcessingService {
    private static final Logger logger = LoggerFactory.getLogger(AsyncProcessingService.class);

    private final ProcessingStateManager processingStateManager;
    private final ScraperOrchestrator scraperOrchestrator;

    public AsyncProcessingService(ProcessingStateManager processingStateManager,
                                  ScraperOrchestrator scraperOrchestrator) {
        this.processingStateManager = processingStateManager;
        this.scraperOrchestrator = scraperOrchestrator;
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
            boolean isAllRegionEnabled,
            boolean isSantiagoEnabled,
            int threadsPerPerson) {
        String firstPersonName = !people.isEmpty() ? people.get(0).getNombres() : "Unknown";

        // Try to acquire lock
        if (!processingStateManager.tryAcquireLock(firstPersonName)) {
            logger.warn("⚠️  System is busy, request {} rejected", requestId);
            return;
        }

        try {
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

            logger.info("✅ Async processing completed for request: {}", requestId);
            logger.info("📊 Found {} results", results.size());

        } catch (Exception e) {
            logger.error("❌ Error during async processing of request {}: {}", 
                requestId, e.getMessage(), e);
        } finally {
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
}

