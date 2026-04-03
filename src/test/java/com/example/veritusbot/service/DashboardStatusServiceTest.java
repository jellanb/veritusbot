package com.example.veritusbot.service;

import com.example.veritusbot.dto.DashboardStatusResponseDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DashboardStatusServiceTest {

    @Test
    void shouldBuildDashboardStatusWithInMemoryMetrics() {
        DashboardStatusService service = new DashboardStatusService();

        service.beginSearch("job-2026-03-001", "Busqueda marzo", 4, true, true);
        service.markSantiagoProgress();
        service.markSantiagoProgress();
        service.markRegionesProgress();
        service.addFoundResults(3);

        DashboardStatusResponseDTO status = service.getStatus(true);

        assertTrue(status.getBackend().isWorking());
        assertEquals("job-2026-03-001", status.getBackend().getActiveSearchId());
        assertEquals("Busqueda marzo", status.getBackend().getActiveSearchName());
        assertNotNull(status.getBackend().getStartedAt());
        assertNotNull(status.getBackend().getLastUpdatedAt());

        assertEquals(50, status.getSearchTypes().getSantiagoCount());
        assertEquals(25, status.getSearchTypes().getRegionesCount());

        assertEquals(1, status.getWorkload().getActiveSearches());
        assertEquals(3, status.getWorkload().getFoundResults());
    }

    @Test
    void shouldKeepFoundResultsAndRequestCountAfterSearchFinished() {
        DashboardStatusService service = new DashboardStatusService();

        service.beginSearch("job-2", "Busqueda 2", 2, true, false);
        service.markSantiagoProgress();
        service.addFoundResults(2);
        service.finishSearch();

        DashboardStatusResponseDTO status = service.getStatus(false);

        assertFalse(status.getBackend().isWorking());
        assertNull(status.getBackend().getActiveSearchId());
        assertNull(status.getBackend().getActiveSearchName());

        assertEquals(0, status.getSearchTypes().getSantiagoCount());
        assertEquals(0, status.getSearchTypes().getRegionesCount());

        assertEquals(1, status.getWorkload().getActiveSearches());
        assertEquals(2, status.getWorkload().getFoundResults());
    }
}

