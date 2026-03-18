package com.example.veritusbot.service.scraper.retry;

/**
 * Strategy interface for handling retry logic when errors occur during scraping
 * Implements Strategy pattern for flexibility and extensibility
 * 
 * SOLID Principle: Open/Closed - Extensible without modifying Phase scrapers
 */
public interface RetryStrategy {
    
    /**
     * Determine if an exception should trigger a retry
     * @param exception The exception that occurred
     * @return true if should retry, false otherwise
     */
    boolean shouldRetry(Exception exception);
    
    /**
     * Get the backoff time in milliseconds before the next retry attempt
     * @param attemptNumber The current attempt number (1-based)
     * @return Milliseconds to wait before retry
     */
    long getBackoffMs(int attemptNumber);
    
    /**
     * Get the maximum number of retry attempts allowed
     * @return Maximum retry attempts
     */
    int getMaxRetries();
    
    /**
     * Determine if the exception is specifically a browser closure error
     * @param exception The exception that occurred
     * @return true if browser was closed, false otherwise
     */
    boolean isBrowserClosureError(Exception exception);
}

