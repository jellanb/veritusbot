package com.example.veritusbot.service.scraper.browser;

import com.example.veritusbot.config.ResourceCleanupManager;
import com.microsoft.playwright.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class BrowserManager {
    private static final Logger logger = LoggerFactory.getLogger(BrowserManager.class);

    private final ResourceCleanupManager resourceCleanupManager;

    public BrowserManager(ResourceCleanupManager resourceCleanupManager) {
        this.resourceCleanupManager = resourceCleanupManager;
    }

    /**
     * Launch a new Chromium browser instance
     * @return Page instance ready to use
     */
    public Page launchBrowser() {
        Playwright playwright = null;
        Browser browser = null;
        
        try {
            // Create NEW instances each time (don't reuse old ones)
            playwright = Playwright.create();
            resourceCleanupManager.registerPlaywright(playwright);
            
            browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions().setHeadless(false).setTimeout(90000)
            );
            resourceCleanupManager.registerBrowser(browser);
            
            Page page = browser.newPage();
            resourceCleanupManager.registerPage(page);
            
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
            page.navigate(url);
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
                // Close page first
                page.close();
                resourceCleanupManager.unregisterPage(page);
                
                // Then close context
                try {
                    Browser browser = page.context().browser();
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
}

