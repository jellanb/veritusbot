package com.example.veritusbot.service.scraper.browser;

import com.example.veritusbot.config.ResourceCleanupManager;
import com.example.veritusbot.service.scraper.config.ScraperConfig;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.Proxy;
import com.microsoft.playwright.options.WaitUntilState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class BrowserManager {
    private static final Logger logger = LoggerFactory.getLogger(BrowserManager.class);

    @Value("${app.scraper.browser.headless:true}")
    private boolean headlessBrowser;

    private final ResourceCleanupManager resourceCleanupManager;
    private final HumanBehaviorService humanBehaviorService;
    private final ProxySelectorService proxySelectorService;
    private final ConcurrentMap<Page, Path> pageSessionPaths = new ConcurrentHashMap<>();
    private final ConcurrentMap<Page, String> pageProxyLabels = new ConcurrentHashMap<>();

    public BrowserManager(ResourceCleanupManager resourceCleanupManager,
                          HumanBehaviorService humanBehaviorService,
                          ProxySelectorService proxySelectorService) {
        this.resourceCleanupManager = resourceCleanupManager;
        this.humanBehaviorService = humanBehaviorService;
        this.proxySelectorService = proxySelectorService;
    }

    /**
     * Launch a new Chromium browser instance
     * @return Page instance ready to use
     */
    public Page launchBrowser() {
        return launchBrowser("default-client");
    }

    /**
     * Launch a new Chromium browser instance with client session persistence
     * @param clientKey Stable key to reuse cookies/storage state
     * @return Page instance ready to use
     */
    public Page launchBrowser(String clientKey) {
        Playwright playwright = null;
        Browser browser = null;
        BrowserContext context = null;
        String safeClientKey = sanitizeClientKey(clientKey);
        Path sessionPath = getSessionPath(safeClientKey);
        
        try {
            // Create NEW instances each time (don't reuse old ones)
            playwright = Playwright.create();
            resourceCleanupManager.registerPlaywright(playwright);
            
            ProxySelectorService.ProxyConfig selectedProxy = proxySelectorService.pickRandomProxyOrNull();
            BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                    .setHeadless(headlessBrowser)
                    .setTimeout(90000);

            if (selectedProxy != null) {
                Proxy proxy = new Proxy(selectedProxy.server());
                if (selectedProxy.username() != null) {
                    proxy.setUsername(selectedProxy.username());
                }
                if (selectedProxy.password() != null) {
                    proxy.setPassword(selectedProxy.password());
                }
                launchOptions.setProxy(proxy);
                logger.info("Using random proxy for this browser instance");
            } else {
                logger.warn("No proxies configured; launching browser without proxy");
            }

            browser = playwright.chromium().launch(launchOptions);
            resourceCleanupManager.registerBrowser(browser);

            Browser.NewContextOptions contextOptions = new Browser.NewContextOptions()
                    .setUserAgent(ScraperConfig.BROWSER_USER_AGENT)
                    .setLocale(ScraperConfig.BROWSER_LOCALE)
                    .setTimezoneId(ScraperConfig.BROWSER_TIMEZONE)
                    .setViewportSize(ScraperConfig.BROWSER_VIEWPORT_WIDTH, ScraperConfig.BROWSER_VIEWPORT_HEIGHT)
                    .setExtraHTTPHeaders(Map.of(
                            "Accept", ScraperConfig.BROWSER_ACCEPT,
                            "Accept-Language", ScraperConfig.BROWSER_ACCEPT_LANGUAGE
                    ));

            if (Files.exists(sessionPath)) {
                contextOptions.setStorageStatePath(sessionPath);
                logger.debug("♻ Reusing session state for {}", safeClientKey);
            }

            context = browser.newContext(contextOptions);
            context.addInitScript("Object.defineProperty(navigator, 'platform', { get: () => '" + ScraperConfig.BROWSER_PLATFORM + "' });");
            resourceCleanupManager.registerBrowserContext(context);
            
            Page page = context.newPage();
            resourceCleanupManager.registerPage(page);
            pageSessionPaths.put(page, sessionPath);
            pageProxyLabels.put(page, selectedProxy != null ? selectedProxy.server() : "no-proxy");
            
            logger.info("✓ Browser launched successfully");
            return page;
        } catch (Exception e) {
            logger.error("❌ Error launching browser: ", e);
            // Clean up on failure
            if (browser != null) {
                try { 
                    browser.close(); 
                } catch (Exception ex) { 
                    logger.error("Error closing browser after failure", ex); 
                }
            }
            if (context != null) {
                try {
                    context.close();
                } catch (Exception ex) {
                    logger.error("Error closing context after failure", ex);
                }
            }
            if (playwright != null) {
                try { 
                    playwright.close(); 
                } catch (Exception ex) { 
                    logger.error("Error closing playwright after failure", ex); 
                }
            }
            throw new RuntimeException("Failed to launch browser", e);
        }
    }

    /**
     * Navigate to a specific URL
     * @param page Page instance
     * @param url Target URL
     */
    public void navigateTo(Page page, String url) {
        try {
            logger.info("📄 Navigating to: {}", url);
            page.navigate(url, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
            humanBehaviorService.waitForDomAndNetwork(page);
            humanBehaviorService.pauseShort(page);
            humanBehaviorService.gradualScroll(page);
            logger.info("✓ Navigation completed");
        } catch (PlaywrightException e) {
            logger.error("❌ Error navigating to URL: {}", url, e);
            throw new RuntimeException("Failed to navigate to URL: " + url, e);
        }
    }

    /**
     * Close browser and cleanup resources
     */
    public void closeBrowser(Page page) {
        try {
            if (page != null) {
                BrowserContext context = page.context();
                Path sessionPath = pageSessionPaths.remove(page);
                pageProxyLabels.remove(page);

                if (sessionPath != null && context != null) {
                    persistSessionState(context, sessionPath);
                }

                // Close page first
                page.close();
                resourceCleanupManager.unregisterPage(page);
                
                // Then close context
                try {
                    if (context != null) {
                        context.close();
                        resourceCleanupManager.unregisterBrowserContext(context);
                    }

                    Browser browser = context != null ? context.browser() : null;
                    if (browser != null) {
                        browser.close();
                        resourceCleanupManager.unregisterBrowser(browser);
                    }
                } catch (Exception e) {
                    logger.debug("Browser already closed: {}", e.getMessage());
                }
                
                logger.info("✓ Browser closed");
            }
        } catch (Exception e) {
            logger.error("❌ Error closing browser: ", e);
        }
    }

    /**
     * Get proxy label associated with a page/browser instance.
     * @param page Page instance
     * @return Proxy server label or fallback value
     */
    public String getProxyLabel(Page page) {
        if (page == null) {
            return "unknown-proxy";
        }
        return pageProxyLabels.getOrDefault(page, "unknown-proxy");
    }

    private void persistSessionState(BrowserContext context, Path sessionPath) {
        try {
            Files.createDirectories(sessionPath.getParent());
            context.storageState(new BrowserContext.StorageStateOptions().setPath(sessionPath));
        } catch (Exception e) {
            logger.warn("Could not persist session state in {}: {}", sessionPath, e.getMessage());
        }
    }

    private Path getSessionPath(String safeClientKey) {
        return Paths.get(ScraperConfig.SESSION_STATE_DIR, safeClientKey + ".json");
    }

    private String sanitizeClientKey(String value) {
        if (value == null || value.isBlank()) {
            return "default-client";
        }
        return value.toLowerCase().replaceAll("[^a-z0-9._-]", "_");
    }
}

