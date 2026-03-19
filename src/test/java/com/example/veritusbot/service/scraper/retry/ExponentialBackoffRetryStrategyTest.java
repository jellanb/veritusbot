package com.example.veritusbot.service.scraper.retry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ExponentialBackoffRetryStrategyTest {

    private final ExponentialBackoffRetryStrategy strategy = new ExponentialBackoffRetryStrategy();

    @Test
    void shouldRetryWhenIOException() {
        assertTrue(strategy.shouldRetry(new IOException("network timeout")));
    }

    @Test
    void shouldRetryWhenBrowserClosurePatternFound() {
        Exception exception = new Exception("Target page, context or browser has been closed");

        assertTrue(strategy.shouldRetry(exception));
    }

    @Test
    void shouldNotRetryForGenericExceptionWithoutKnownPattern() {
        assertFalse(strategy.shouldRetry(new Exception("validation failed")));
    }

    @Test
    void shouldReturnConfiguredMaxRetries() {
        assertEquals(5, strategy.getMaxRetries());
    }

    @ParameterizedTest
    @MethodSource("backoffCases")
    void shouldCalculateExponentialBackoff(int attemptNumber, long expectedMs) {
        assertEquals(expectedMs, strategy.getBackoffMs(attemptNumber));
    }

    @ParameterizedTest
    @MethodSource("browserClosurePatternCases")
    void shouldDetectBrowserClosureErrors(String message) {
        assertTrue(strategy.isBrowserClosureError(new Exception(message)));
    }

    @Test
    void shouldDetectBrowserClosureByExceptionTypeName() {
        Exception playwrightLikeException = new CustomPlaywrightException("Some unrelated message");

        assertTrue(strategy.isBrowserClosureError(playwrightLikeException));
    }

    @Test
    void shouldReturnFalseWhenExceptionIsNull() {
        assertFalse(strategy.isBrowserClosureError(null));
    }

    @Test
    void shouldReturnFalseWhenMessageIsNull() {
        assertFalse(strategy.isBrowserClosureError(new Exception((String) null)));
    }

    private static Stream<Arguments> backoffCases() {
        return Stream.of(
                Arguments.of(1, 1000L),
                Arguments.of(2, 2000L),
                Arguments.of(3, 4000L),
                Arguments.of(4, 8000L),
                Arguments.of(5, 16000L)
        );
    }

    private static Stream<Arguments> browserClosurePatternCases() {
        return Stream.of(
                Arguments.of("Target page, context or browser has been closed"),
                Arguments.of("Target closed"),
                Arguments.of("Browser closed"),
                Arguments.of("Connection closed"),
                Arguments.of("Protocol error"),
                Arguments.of("WebSocket is closed"),
                Arguments.of("ERR_FAILED"),
                Arguments.of("net::ERR_CONNECTION_RESET")
        );
    }

    private static class CustomPlaywrightException extends Exception {
        CustomPlaywrightException(String message) {
            super(message);
        }
    }
}

