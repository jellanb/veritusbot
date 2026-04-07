package com.example.veritusbot.service.scraper.retry;

import java.util.HashSet;
import java.util.Set;

/**
 * Exception thrown by Phase scrapers when an error occurs.
 * ScraperOrchestrator uses this to determine if the operation should be retried.
 * 
 * SOLID Principles:
 * - Single Responsibility: Encapsulates retry decision logic
 * - Open/Closed: Extensible for new error types without modifying existing code
 */
public class RetryableScraperException extends Exception {
    
    private final boolean isRetryable;
    private final String context;  // e.g., "tribunal: Juzgado de Letras Civil" or "year: 2020"
    private final Integer failedTribunalPosition;
    private final String failedTribunalName;  // exact name of the tribunal that failed

    /**
     * Create a retryable scraper exception
     *
     * @param message Human-readable error message
     * @param cause Original exception that caused this error
     * @param isRetryable Whether ScraperOrchestrator should retry this operation
     * @param context Additional context (tribunal name, year, etc.)
     */
    public RetryableScraperException(String message, Throwable cause, boolean isRetryable, String context) {
        this(message, cause, isRetryable, context, null, null);
    }

    /**
     * Create a retryable scraper exception with tribunal checkpoint info.
     *
     * @param message Human-readable error message
     * @param cause Original exception that caused this error
     * @param isRetryable Whether ScraperOrchestrator should retry this operation
     * @param context Additional context (tribunal name, year, etc.)
     * @param failedTribunalPosition zero-based tribunal position where failure occurred
     */
    public RetryableScraperException(String message,
                                     Throwable cause,
                                     boolean isRetryable,
                                     String context,
                                     Integer failedTribunalPosition) {
        this(message, cause, isRetryable, context, failedTribunalPosition, null);
    }

    /**
     * Create a retryable scraper exception with full tribunal context for name-based retry.
     *
     * @param message Human-readable error message
     * @param cause Original exception that caused this error
     * @param isRetryable Whether ScraperOrchestrator should retry this operation
     * @param context Additional context (tribunal name, year, etc.)
     * @param failedTribunalPosition zero-based tribunal position where failure occurred
     * @param failedTribunalName exact name of the tribunal that failed (used for name-based retry)
     */
    public RetryableScraperException(String message,
                                     Throwable cause,
                                     boolean isRetryable,
                                     String context,
                                     Integer failedTribunalPosition,
                                     String failedTribunalName) {
        super(message, cause);
        this.isRetryable = isRetryable;
        this.context = context;
        this.failedTribunalPosition = failedTribunalPosition;
        this.failedTribunalName = failedTribunalName;
    }

    /**
     * Determine if error is due to browser closure or network issues (retryable)
     * 
     * SOLID Principle: Single Responsibility
     * This static utility is placed here because it determines retry logic for browser/network errors
     * which is the core responsibility of this exception class.
     * 
     * @param cause The exception to check
     * @return true if the error is browser/network related and should be retried
     */
    public static boolean isBrowserOrNetworkError(Throwable cause) {
        Set<Throwable> visited = new HashSet<>();
        Throwable current = cause;

        while (current != null && visited.add(current)) {
            if (isKnownRetryableThrowable(current)) {
                return true;
            }
            current = current.getCause();
        }

        return false;
    }

    private static boolean isKnownRetryableThrowable(Throwable cause) {
        if (cause == null) {
            return false;
        }

        String message = cause.getMessage() != null ? cause.getMessage() : "";
        String simpleName = cause.getClass().getSimpleName();
        String className = cause.getClass().getName();

        return message.contains("Target page, context or browser has been closed") ||
               message.contains("Target closed") ||
               message.contains("Browser closed") ||
               message.contains("Connection closed") ||
               message.contains("Protocol error") ||
               message.contains("WebSocket is closed") ||
               message.contains("ERR_FAILED") ||
               message.contains("net::ERR") ||
               message.contains("waiting for locator(") ||
               simpleName.contains("PlaywrightException") ||
               simpleName.contains("TimeoutError") ||
               className.startsWith("com.microsoft.playwright.") ||
               cause instanceof java.io.IOException;
    }

    /**
     * @return true if ScraperOrchestrator should retry this operation with a new browser
     */
    public boolean isRetryable() {
        return isRetryable;
    }

    /**
     * @return Context about where/what failed (e.g., tribunal name, year)
     */
    public String getContext() {
        return context;
    }

    /**
     * @return zero-based tribunal position where failure occurred, or null if not applicable
     */
    public Integer getFailedTribunalPosition() {
        return failedTribunalPosition;
    }

    /**
     * @return exact name of the tribunal that failed, or null if not applicable.
     *         Used by Phase scrapers to resolve the correct resume position by name on retry,
     *         which is more robust than position-based resolution.
     */
    public String getFailedTribunalName() {
        return failedTribunalName;
    }
}


