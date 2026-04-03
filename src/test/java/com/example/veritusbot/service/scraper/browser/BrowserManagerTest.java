package com.example.veritusbot.service.scraper.browser;

import com.example.veritusbot.config.ResourceCleanupManager;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Map;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrowserManagerTest {

    @Mock
    private ResourceCleanupManager resourceCleanupManager;

    @Mock
    private HumanBehaviorService humanBehaviorService;

    @Mock
    private ProxySelectorService proxySelectorService;

    @Mock
    private Page page;

    @Mock
    private BrowserContext context;

    @Test
    void closeBrowserShouldReleaseExclusiveProxyEvenWhenContextCloseFails() throws Exception {
        BrowserManager manager = new BrowserManager(resourceCleanupManager, humanBehaviorService, proxySelectorService);
        String proxyServer = "http://10.0.0.1:8080";

        when(page.context()).thenReturn(context);
        doThrow(new RuntimeException("context close failed")).when(context).close();
        registerPageProxyLabel(manager, page, proxyServer);

        manager.closeBrowser(page);

        verify(proxySelectorService).releaseExclusiveProxy(proxyServer);
    }

    @Test
    void closeBrowserShouldReleaseExclusiveProxyEvenWhenPageCloseFails() throws Exception {
        BrowserManager manager = new BrowserManager(resourceCleanupManager, humanBehaviorService, proxySelectorService);
        String proxyServer = "http://10.0.0.2:8080";

        when(page.context()).thenReturn(context);
        doThrow(new RuntimeException("page close failed")).when(page).close();
        registerPageProxyLabel(manager, page, proxyServer);

        manager.closeBrowser(page);

        verify(proxySelectorService).releaseExclusiveProxy(proxyServer);
    }

    @SuppressWarnings("unchecked")
    private void registerPageProxyLabel(BrowserManager manager, Page page, String proxyServer) throws Exception {
        Field labelsField = BrowserManager.class.getDeclaredField("pageProxyLabels");
        labelsField.setAccessible(true);
        ((Map<Page, String>) labelsField.get(manager)).put(page, proxyServer);

        Field sessionField = BrowserManager.class.getDeclaredField("pageSessionPaths");
        sessionField.setAccessible(true);
        ((Map<Page, Path>) sessionField.get(manager)).put(page, Path.of("target", "test-session.json"));
    }
}


