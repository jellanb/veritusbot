package com.example.veritusbot.service.scraper.retry;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RetryableScraperExceptionTest {

    @Test
    void shouldTreatWrappedPlaywrightTimeoutAsRetryable() {
        RuntimeException wrapped = new RuntimeException(
                "Failed to submit form using selector button[type='submit']#btnConConsultaNom",
                new TimeoutError("Timeout 90000ms exceeded while waiting for locator('#btnConConsultaNom')"));

        assertTrue(RetryableScraperException.isBrowserOrNetworkError(wrapped));
    }

    @Test
    void shouldTreatWrappedPlaywrightStyleExceptionAsRetryable() {
        RuntimeException wrapped = new RuntimeException(
                "wrapper",
                new FakePlaywrightException("Target page, context or browser has been closed"));

        assertTrue(RetryableScraperException.isBrowserOrNetworkError(wrapped));
    }

    @Test
    void shouldNotTreatGenericWrappedValidationErrorAsRetryable() {
        RuntimeException wrapped = new RuntimeException(
                "Failed to submit form",
                new IllegalArgumentException("validation failed"));

        assertFalse(RetryableScraperException.isBrowserOrNetworkError(wrapped));
    }

    private static class TimeoutError extends RuntimeException {
        TimeoutError(String message) {
            super(message);
        }
    }

    private static class FakePlaywrightException extends RuntimeException {
        FakePlaywrightException(String message) {
            super(message);
        }
    }
}

