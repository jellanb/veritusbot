package com.example.veritusbot.service;

import com.example.veritusbot.dto.DashboardStatusResponseDTO;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Mantiene métricas de dashboard en memoria durante la ejecución del backend.
 */
@Service
public class DashboardStatusService {

    private final Object lock = new Object();
    private final AtomicInteger totalAcceptedSearchRequests = new AtomicInteger(0);
    private final AtomicInteger foundPositiveResults = new AtomicInteger(0);

    private String activeSearchId;
    private String activeSearchName;
    private Instant startedAt;
    private Instant lastUpdatedAt;
    private int totalPeople;
    private int processedSantiagoPeople;
    private int processedRegionesPeople;
    private boolean santiagoEnabled;
    private boolean regionesEnabled;

    public void beginSearch(String searchId,
                            String searchName,
                            int peopleCount,
                            boolean isSantiagoEnabled,
                            boolean isRegionesEnabled) {
        synchronized (lock) {
            totalAcceptedSearchRequests.incrementAndGet();
            activeSearchId = searchId;
            activeSearchName = searchName;
            totalPeople = Math.max(peopleCount, 0);
            processedSantiagoPeople = 0;
            processedRegionesPeople = 0;
            santiagoEnabled = isSantiagoEnabled;
            regionesEnabled = isRegionesEnabled;
            startedAt = Instant.now();
            lastUpdatedAt = startedAt;
        }
    }

    public void markSantiagoProgress() {
        synchronized (lock) {
            if (activeSearchId == null || !santiagoEnabled) {
                return;
            }
            processedSantiagoPeople++;
            lastUpdatedAt = Instant.now();
        }
    }

    public void markRegionesProgress() {
        synchronized (lock) {
            if (activeSearchId == null || !regionesEnabled) {
                return;
            }
            processedRegionesPeople++;
            lastUpdatedAt = Instant.now();
        }
    }

    public void addFoundResults(int foundCount) {
        if (foundCount <= 0) {
            return;
        }
        foundPositiveResults.addAndGet(foundCount);
        synchronized (lock) {
            lastUpdatedAt = Instant.now();
        }
    }

    public void finishSearch() {
        synchronized (lock) {
            lastUpdatedAt = Instant.now();
            activeSearchId = null;
            activeSearchName = null;
            startedAt = null;
            totalPeople = 0;
            processedSantiagoPeople = 0;
            processedRegionesPeople = 0;
            santiagoEnabled = false;
            regionesEnabled = false;
        }
    }

    public DashboardStatusResponseDTO getStatus(boolean isWorking) {
        synchronized (lock) {
            DashboardStatusResponseDTO.BackendStatusDTO backend = new DashboardStatusResponseDTO.BackendStatusDTO(
                    isWorking,
                    activeSearchId,
                    activeSearchName,
                    startedAt != null ? startedAt.toString() : null,
                    lastUpdatedAt != null ? lastUpdatedAt.toString() : null
            );

            DashboardStatusResponseDTO.SearchTypesStatusDTO searchTypes =
                    new DashboardStatusResponseDTO.SearchTypesStatusDTO(
                            calculateProgressPercentage(processedSantiagoPeople, totalPeople, santiagoEnabled),
                            calculateProgressPercentage(processedRegionesPeople, totalPeople, regionesEnabled)
                    );

            DashboardStatusResponseDTO.WorkloadStatusDTO workload =
                    new DashboardStatusResponseDTO.WorkloadStatusDTO(
                            totalAcceptedSearchRequests.get(),
                            foundPositiveResults.get()
                    );

            return new DashboardStatusResponseDTO(backend, searchTypes, workload);
        }
    }

    private int calculateProgressPercentage(int processed, int total, boolean enabled) {
        if (!enabled || total <= 0) {
            return 0;
        }
        if (processed >= total) {
            return 100;
        }
        return (int) Math.round((processed * 100.0) / total);
    }
}

