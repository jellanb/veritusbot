package com.example.veritusbot.service.scraper.browser;

import com.microsoft.playwright.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class BrowserManager {
    private static final Logger logger = LoggerFactory.getLogger(BrowserManager.class);

    private Browser browser;
    private Playwright playwright;

    /**
     * Launch a new Chromium browser instance
     * @return Page instance ready to use
     */
    public Page launchBrowser() {
        try {
            playwright = Playwright.create();
            browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions().setHeadless(false).setTimeout(90000)
            );
            Page page = browser.newPage();
            logger.info("✓ Browser launched successfully");
            return page;
        } catch (Exception e) {
            logger.error("❌ Error launching browser: ", e);
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
    public void closeBrowser() {
        try {
            if (browser != null) {
                browser.close();
                logger.info("✓ Browser closed");
            }
            if (playwright != null) {
                playwright.close();
                logger.info("✓ Playwright closed");
            }
        } catch (Exception e) {
            logger.error("❌ Error closing browser: ", e);
        }
    }
}

