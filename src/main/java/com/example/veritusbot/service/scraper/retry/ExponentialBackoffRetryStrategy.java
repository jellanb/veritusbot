package com.example.veritusbot.service.scraper.retry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Implementation of RetryStrategy using exponential backoff
 * 
 * Backoff timing: 1s, 2s, 4s, 8s, 16s (2^(attempt-1) seconds)
 * 
 * SOLID Principles:
 * - Single Responsibility: Only handles retry strategy logic
 * - Open/Closed: Can be extended for different backoff strategies
 * - Dependency Inversion: Implements RetryStrategy interface
 */
@Component
public class ExponentialBackoffRetryStrategy implements RetryStrategy {
    private static final Logger logger = LoggerFactory.getLogger(ExponentialBackoffRetryStrategy.class);
    
    // Configuration constants
    private static final int MAX_RETRIES = 5;
    private static final long INITIAL_BACKOFF_MS = 1000;  // 1 second
    
    @Override
    public boolean shouldRetry(Exception exception) {
        // Retry if it's a browser closure error OR an IOException (network issue)
        return isBrowserClosureError(exception) || exception instanceof java.io.IOException;
    }
    
    @Override
    public long getBackoffMs(int attemptNumber) {
        // Exponential backoff: 2^(attemptNumber - 1) * 1000ms
        // Attempt 1: 1s, Attempt 2: 2s, Attempt 3: 4s, Attempt 4: 8s, Attempt 5: 16s
        return (long) Math.pow(2, attemptNumber - 1) * INITIAL_BACKOFF_MS;
    }
    
    @Override
    public int getMaxRetries() {
        return MAX_RETRIES;
    }
    
    @Override
    public boolean isBrowserClosureError(Exception exception) {
        if (exception == null || exception.getMessage() == null) {
            return false;
        }
        
        String errorMsg = exception.getMessage();
        String errorClass = exception.getClass().getSimpleName();
        
        // Check for browser closure error patterns
        return errorMsg.contains("Target page, context or browser has been closed") ||
               errorMsg.contains("Target closed") ||
               errorMsg.contains("Browser closed") ||
               errorMsg.contains("Connection closed") ||
               errorMsg.contains("Protocol error") ||
               errorMsg.contains("WebSocket is closed") ||
               errorMsg.contains("ERR_FAILED") ||
               errorMsg.contains("net::ERR") ||
               errorClass.contains("PlaywrightException");
    }
}

