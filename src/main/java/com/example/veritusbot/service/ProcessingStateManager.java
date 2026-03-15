package com.example.veritusbot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Service responsible for managing the processing state of the application
 * Implements Single Responsibility Principle: only manages busy/idle state
 */
@Service
public class ProcessingStateManager {
    private static final Logger logger = LoggerFactory.getLogger(ProcessingStateManager.class);

    private volatile boolean isProcessing = false;
    private final ReentrantLock lock = new ReentrantLock();
    private LocalDateTime lastProcessingStart;
    private LocalDateTime lastProcessingEnd;
    private String currentProcessingPerson;
    private int totalRequests = 0;
    private int completedRequests = 0;

    /**
     * Try to acquire processing lock
     * Returns true if lock acquired, false if already processing
     * 
     * @param personName Name of person being processed
     * @return true if lock acquired, false if busy
     */
    public boolean tryAcquireLock(String personName) {
        if (lock.tryLock()) {
            try {
                if (isProcessing) {
                    // Already processing, release and return false
                    return false;
                }

                // Mark as processing
                isProcessing = true;
                currentProcessingPerson = personName;
                lastProcessingStart = LocalDateTime.now();
                totalRequests++;

                logger.info("🔒 Lock acquired for processing: {}", personName);
                return true;

            } finally {
                lock.unlock();
            }
        }

        logger.warn("⚠️  Could not acquire lock (busy)");
        return false;
    }

    /**
     * Release processing lock
     */
    public void releaseLock() {
        lock.lock();
        try {
            if (isProcessing) {
                isProcessing = false;
                lastProcessingEnd = LocalDateTime.now();
                completedRequests++;

                long processingTimeMs = calculateProcessingTime();
                logger.info("🔓 Lock released. Processing time: {}ms", processingTimeMs);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Check if system is currently processing
     * 
     * @return true if processing, false if idle
     */
    public boolean isProcessing() {
        return isProcessing;
    }

    /**
     * Get the name of person currently being processed
     * 
     * @return Person name or null if idle
     */
    public String getCurrentProcessingPerson() {
        return currentProcessingPerson;
    }

    /**
     * Get processing state information
     * 
     * @return ProcessingState object with current state
     */
    public ProcessingState getState() {
        return new ProcessingState(
                isProcessing,
                currentProcessingPerson,
                lastProcessingStart,
                lastProcessingEnd,
                totalRequests,
                completedRequests,
                calculateProcessingTime()
        );
    }

    /**
     * Calculate current or last processing time in milliseconds
     * 
     * @return Processing time in ms
     */
    private long calculateProcessingTime() {
        if (lastProcessingStart == null) {
            return 0;
        }

        LocalDateTime endTime = lastProcessingEnd != null ? lastProcessingEnd : LocalDateTime.now();
        return java.time.temporal.ChronoUnit.MILLIS.between(lastProcessingStart, endTime);
    }

    /**
     * Reset statistics (for testing)
     */
    public void resetStatistics() {
        lock.lock();
        try {
            totalRequests = 0;
            completedRequests = 0;
            lastProcessingStart = null;
            lastProcessingEnd = null;
            currentProcessingPerson = null;
            logger.info("📊 Statistics reset");
        } finally {
            lock.unlock();
        }
    }

    /**
     * Inner class to represent processing state
     */
    public static class ProcessingState {
        private final boolean isProcessing;
        private final String currentPerson;
        private final LocalDateTime startTime;
        private final LocalDateTime endTime;
        private final int totalRequests;
        private final int completedRequests;
        private final long processingTimeMs;

        public ProcessingState(boolean isProcessing, String currentPerson, LocalDateTime startTime,
                              LocalDateTime endTime, int totalRequests, int completedRequests,
                              long processingTimeMs) {
            this.isProcessing = isProcessing;
            this.currentPerson = currentPerson;
            this.startTime = startTime;
            this.endTime = endTime;
            this.totalRequests = totalRequests;
            this.completedRequests = completedRequests;
            this.processingTimeMs = processingTimeMs;
        }

        // Getters
        public boolean isProcessing() { return isProcessing; }
        public String getCurrentPerson() { return currentPerson; }
        public LocalDateTime getStartTime() { return startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public int getTotalRequests() { return totalRequests; }
        public int getCompletedRequests() { return completedRequests; }
        public long getProcessingTimeMs() { return processingTimeMs; }
    }
}

