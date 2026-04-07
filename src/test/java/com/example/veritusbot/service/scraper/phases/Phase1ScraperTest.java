package com.example.veritusbot.service.scraper.phases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.veritusbot.dto.PersonaDTO;
import com.example.veritusbot.service.ResultPersistenceService;
import com.example.veritusbot.service.TribunalBusquedaService;
import com.example.veritusbot.service.scraper.TribunalTrackingContext;
import com.example.veritusbot.service.scraper.browser.BrowserManager;
import com.example.veritusbot.service.scraper.browser.FrameNavigator;
import com.example.veritusbot.service.scraper.browser.HumanBehaviorService;
import com.example.veritusbot.service.scraper.form.FormFiller;
import com.example.veritusbot.service.scraper.form.TribunalSelector;
import com.example.veritusbot.service.scraper.parser.ResultParser;
import com.example.veritusbot.service.scraper.retry.RetryableScraperException;
import com.microsoft.playwright.Frame;
import com.microsoft.playwright.Page;

class Phase1ScraperTest {

    private FormFiller formFiller;
    private TribunalSelector tribunalSelector;
    private ResultParser resultParser;
    private FrameNavigator frameNavigator;
    private ResultPersistenceService resultPersistenceService;
    private HumanBehaviorService humanBehaviorService;
    private BrowserManager browserManager;
    private TribunalBusquedaService tribunalBusquedaService;
    private Phase1Scraper phase1Scraper;

    private Page page;
    private Frame frame;

    @BeforeEach
    void setUp() {
        formFiller = mock(FormFiller.class);
        tribunalSelector = mock(TribunalSelector.class);
        resultParser = mock(ResultParser.class);
        frameNavigator = mock(FrameNavigator.class);
        resultPersistenceService = mock(ResultPersistenceService.class);
        humanBehaviorService = mock(HumanBehaviorService.class);
        browserManager = mock(BrowserManager.class);
        tribunalBusquedaService = mock(TribunalBusquedaService.class);
        page = mock(Page.class);
        frame = mock(Frame.class);

        phase1Scraper = new Phase1Scraper(
                formFiller,
                tribunalSelector,
                resultParser,
                frameNavigator,
                resultPersistenceService,
                humanBehaviorService,
                browserManager,
                tribunalBusquedaService);
    }

    @Test
    void shouldWrapSubmitTimeoutAsRetryableScraperException() {
        PersonaDTO person = new PersonaDTO("Ana Maria", "Perez", "Lopez", 2023, 2023);
        TribunalTrackingContext trackingContext = new TribunalTrackingContext(1, "REQ-1", "PHASE_1");

        when(browserManager.getProxyLabel(page)).thenReturn("proxy-a");
        when(frameNavigator.getSearchFrame(page)).thenReturn(frame);

        Map<String, Integer> tribunals = new LinkedHashMap<>();
        tribunals.put("1er Juzgado Civil de Santiago", 10);
        tribunals.put("Corte de Apelaciones de Valparaiso", 20);
        when(tribunalSelector.loadAllTribunals(frame)).thenReturn(tribunals);

        doNothing().when(frameNavigator).navigateToSearchForm(page);
        doNothing().when(frameNavigator).clickSearchByNameTab(frame, page);
        doNothing().when(frameNavigator).setCompetenceToCivil(frame, page);
        doNothing().when(formFiller).fillSearchForm(frame, person, 2023);
        doNothing().when(tribunalSelector).openDropdown(frame);
        doNothing().when(tribunalSelector).closeDropdown(frame);
        doNothing().when(tribunalSelector).selectTribunal(frame, "1er Juzgado Civil de Santiago", 10);
        doNothing().when(humanBehaviorService).pauseShort(page);
        doNothing().when(humanBehaviorService).pause(page, 250, 650);
        doNothing().when(humanBehaviorService).pause(page, 600, 1600);

        doThrow(new RuntimeException(
                "Failed to submit form using selector button[type='submit']#btnConConsultaNom",
                new TimeoutError("Timeout 90000ms exceeded while waiting for locator('#btnConConsultaNom')")))
                .when(formFiller).submitForm(frame);

        RetryableScraperException exception = assertThrows(
                RetryableScraperException.class,
                () -> phase1Scraper.execute(page, person, 2023, 2023, 0, null, trackingContext));

        assertTrue(exception.isRetryable());
        assertEquals(0, exception.getFailedTribunalPosition());
        assertEquals("1er Juzgado Civil de Santiago", exception.getFailedTribunalName());
    }

    private static class TimeoutError extends RuntimeException {
        TimeoutError(String message) {
            super(message);
        }
    }
}


