package com.example.veritusbot.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.veritusbot.dto.PersonaDTO;
import com.example.veritusbot.dto.ResultDTO;
import com.example.veritusbot.service.scraper.ScraperOrchestrator;

@ExtendWith(MockitoExtension.class)
class AsyncProcessingServiceTest {

    @Mock
    private ProcessingStateManager processingStateManager;

    @Mock
    private ScraperOrchestrator scraperOrchestrator;

    private AsyncProcessingService asyncProcessingService;

    @BeforeEach
    void setUp() {
        asyncProcessingService = new AsyncProcessingService(processingStateManager, scraperOrchestrator);
    }

    @Test
    void processSearchAsyncShouldReturnWhenSystemIsBusy() {
        PersonaDTO person = new PersonaDTO("Ana", "Perez", "Diaz", 2020, 2020);
        when(processingStateManager.tryAcquireLock("Ana")).thenReturn(false);

        asyncProcessingService.processSearchAsync(List.of(person), "REQ-1", true);

        verify(processingStateManager, times(1)).tryAcquireLock("Ana");
        verify(scraperOrchestrator, never()).scrapePeople(anyList(), org.mockito.ArgumentMatchers.anyBoolean());
        verify(processingStateManager, never()).releaseLock();
    }

    @Test
    void processSearchAsyncShouldUseUnknownWhenPeopleListIsEmpty() {
        when(processingStateManager.tryAcquireLock("Unknown")).thenReturn(false);

        asyncProcessingService.processSearchAsync(List.of(), "REQ-EMPTY", true);

        verify(processingStateManager, times(1)).tryAcquireLock("Unknown");
        verify(scraperOrchestrator, never()).scrapePeople(anyList(), org.mockito.ArgumentMatchers.anyBoolean());
    }

    @Test
    void processSearchAsyncShouldScrapeAndReleaseLockWhenSuccessful() {
        PersonaDTO person = new PersonaDTO("Luis", "Gomez", "Rojas", 2021, 2021);
        when(processingStateManager.tryAcquireLock("Luis")).thenReturn(true);
        when(scraperOrchestrator.scrapePeople(List.of(person), true)).thenReturn(List.of(
                new ResultDTO("Luis Gomez Rojas", "Tribunal", 2021, "OK", "detalle")
        ));

        asyncProcessingService.processSearchAsync(List.of(person), "REQ-2", true);

        verify(scraperOrchestrator, times(1)).scrapePeople(List.of(person), true);
        verify(processingStateManager, times(1)).releaseLock();
    }

    @Test
    void processSearchAsyncShouldReleaseLockWhenScraperFails() {
        PersonaDTO person = new PersonaDTO("Maria", "Soto", "Lopez", 2022, 2022);
        when(processingStateManager.tryAcquireLock("Maria")).thenReturn(true);
        doThrow(new RuntimeException("boom")).when(scraperOrchestrator).scrapePeople(List.of(person), false);

        asyncProcessingService.processSearchAsync(List.of(person), "REQ-3", false);

        verify(scraperOrchestrator, times(1)).scrapePeople(List.of(person), false);
        verify(processingStateManager, times(1)).releaseLock();
    }

    @Test
    void isBusyShouldDelegateToProcessingStateManager() {
        when(processingStateManager.isProcessing()).thenReturn(true);

        assertTrue(asyncProcessingService.isBusy());
        verify(processingStateManager, times(1)).isProcessing();

        when(processingStateManager.isProcessing()).thenReturn(false);
        assertFalse(asyncProcessingService.isBusy());
    }

    @Test
    void getStateShouldDelegateToProcessingStateManager() {
        ProcessingStateManager.ProcessingState state = new ProcessingStateManager.ProcessingState(
                true,
                "Ana",
                LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now(),
                3,
                2,
                1200
        );
        when(processingStateManager.getState()).thenReturn(state);

        ProcessingStateManager.ProcessingState result = asyncProcessingService.getState();

        assertSame(state, result);
        verify(processingStateManager, times(1)).getState();
    }
}

