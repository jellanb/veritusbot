package com.example.veritusbot.service.scraper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.veritusbot.dto.PersonaDTO;
import com.example.veritusbot.dto.ResultDTO;
import com.example.veritusbot.model.PersonaProcesada;
import com.example.veritusbot.service.PersonProcessingService;
import com.example.veritusbot.service.PersonaProcesadaPersistenceService;
import com.example.veritusbot.service.ProcessingStateManager;
import com.example.veritusbot.service.scraper.browser.BrowserManager;
import com.example.veritusbot.service.scraper.phases.Phase1Scraper;
import com.example.veritusbot.service.scraper.phases.Phase2Scraper;
import com.example.veritusbot.service.scraper.retry.RetryableScraperException;
import com.microsoft.playwright.Page;

@ExtendWith(MockitoExtension.class)
class ScraperOrchestratorTest {

    @Mock
    private BrowserManager browserManager;

    @Mock
    private Phase1Scraper phase1Scraper;

    @Mock
    private Phase2Scraper phase2Scraper;

    @Mock
    private PersonProcessingService personProcessingService;

    @Mock
    private ProcessingStateManager processingStateManager;

    @Mock
    private PersonaProcesadaPersistenceService personaProcesadaPersistenceService;

    @Mock
    private Page page;

    private ScraperOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new ScraperOrchestrator(
                browserManager,
                phase1Scraper,
                phase2Scraper,
                personProcessingService,
                processingStateManager,
                personaProcesadaPersistenceService);

        ReflectionTestUtils.setField(orchestrator, "maxThreads", 1);
        ReflectionTestUtils.setField(orchestrator, "pjudUrl", "https://example.test/pjud");
    }

    @Test
    void constructorShouldResetProcessingState() {
        verify(processingStateManager, times(1)).resetState();
    }

    @Test
    void scrapePeopleShouldProcessBothPhasesAndMarkPeopleAsProcessed() throws Exception {
        PersonaDTO phase1Person = new PersonaDTO("Ana", "Perez", "Diaz", 2020, 2020);
        PersonaDTO phase2Person = new PersonaDTO("Luis", "Gomez", "Rojas", 2021, 2021);

        ResultDTO phase1Result = new ResultDTO("Ana Perez Diaz", "Tribunal A", 2020, "OK", "detalle");
        ResultDTO phase2Result = new ResultDTO("Luis Gomez Rojas", "Tribunal B", 2021, "OK", "detalle");

        PersonaProcesada phase1Tracking = new PersonaProcesada();
        PersonaProcesada phase2Tracking = new PersonaProcesada();

        when(personProcessingService.filterPeopleForPhase1(any())).thenReturn(List.of(phase1Person));
        when(personProcessingService.filterPeopleForPhase2(any())).thenReturn(List.of(phase2Person));
        when(browserManager.launchBrowser()).thenReturn(page);
        doNothing().when(browserManager).navigateTo(eq(page), any());

        when(phase1Scraper.execute(eq(page), eq(phase1Person), eq(2020), eq(2020)))
                .thenReturn(List.of(phase1Result));
        when(phase2Scraper.execute(eq(page), eq(phase2Person), eq(2021), eq(2021)))
                .thenReturn(List.of(phase2Result));

        when(personaProcesadaPersistenceService.getOrCreatePersonaProcesada(phase1Person)).thenReturn(phase1Tracking);
        when(personaProcesadaPersistenceService.getOrCreatePersonaProcesada(phase2Person)).thenReturn(phase2Tracking);

        List<ResultDTO> results = orchestrator.scrapePeople(List.of(phase1Person, phase2Person));

        assertEquals(2, results.size());
        verify(phase1Scraper, times(1)).execute(eq(page), eq(phase1Person), eq(2020), eq(2020));
        verify(phase2Scraper, times(1)).execute(eq(page), eq(phase2Person), eq(2021), eq(2021));

        verify(personaProcesadaPersistenceService, times(1))
                .markTribunalPrincipalAsProcessed(phase1Tracking);
        verify(personaProcesadaPersistenceService, times(1))
                .markAsProcessed(phase2Tracking);
    }

    @Test
    void scrapePeopleShouldRetryOnRetryableExceptionAndSucceed() throws Exception {
        PersonaDTO person = new PersonaDTO("Maria", "Soto", "Lopez", 2022, 2022);
        ResultDTO retriedResult = new ResultDTO("Maria Soto Lopez", "Tribunal C", 2022, "OK", "detalle");

        Page firstAttemptPage = page;
        Page secondAttemptPage = org.mockito.Mockito.mock(Page.class);

        when(personProcessingService.filterPeopleForPhase1(any())).thenReturn(List.of(person));
        when(personProcessingService.filterPeopleForPhase2(any())).thenReturn(List.of());
        when(browserManager.launchBrowser()).thenReturn(firstAttemptPage, secondAttemptPage);
        doNothing().when(browserManager).navigateTo(any(Page.class), any());

        when(phase1Scraper.execute(any(Page.class), eq(person), eq(2022), eq(2022)))
                .thenThrow(new RetryableScraperException("retry", null, true, "year: 2022"))
                .thenReturn(List.of(retriedResult));

        when(personaProcesadaPersistenceService.getOrCreatePersonaProcesada(person)).thenReturn(new PersonaProcesada());

        List<ResultDTO> results = orchestrator.scrapePeople(List.of(person));

        assertEquals(1, results.size());
        verify(browserManager, times(2)).launchBrowser();
        verify(browserManager, times(2)).navigateTo(any(Page.class), eq("https://example.test/pjud"));
        verify(phase1Scraper, times(2)).execute(any(Page.class), eq(person), eq(2022), eq(2022));
        verify(browserManager, times(1)).closeBrowser(firstAttemptPage);
    }

    @Test
    void scrapePeopleCurrentlyRetriesEvenWhenExceptionIsNonRetryable() throws Exception {
        PersonaDTO person = new PersonaDTO("Tomas", "Rios", "Mena", 2023, 2023);

        when(personProcessingService.filterPeopleForPhase1(any())).thenReturn(List.of(person));
        when(personProcessingService.filterPeopleForPhase2(any())).thenReturn(List.of());
        when(browserManager.launchBrowser()).thenReturn(page);
        doNothing().when(browserManager).navigateTo(eq(page), any());

        when(phase1Scraper.execute(eq(page), eq(person), eq(2023), eq(2023)))
                .thenThrow(new RetryableScraperException("fatal", null, false, "year: 2023"));

        when(personaProcesadaPersistenceService.getOrCreatePersonaProcesada(person)).thenReturn(new PersonaProcesada());

        List<ResultDTO> results = orchestrator.scrapePeople(List.of(person));

        assertEquals(0, results.size());
        verify(browserManager, times(5)).launchBrowser();
        verify(phase1Scraper, times(5)).execute(eq(page), eq(person), eq(2023), eq(2023));
    }
}

