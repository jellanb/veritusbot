package com.example.veritusbot.service.scraper;

/**
 * Immutable context required to track tribunal searches for a person and request.
 */
public record TribunalTrackingContext(
        Integer personaProcesadaId,
        String requestId,
        String faseCodigo
) {
}

