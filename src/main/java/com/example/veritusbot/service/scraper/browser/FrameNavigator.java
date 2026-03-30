package com.example.veritusbot.service.scraper.browser;

import com.microsoft.playwright.Frame;
import com.microsoft.playwright.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FrameNavigator {
    private static final Logger logger = LoggerFactory.getLogger(FrameNavigator.class);
    private final HumanBehaviorService humanBehaviorService;

    public FrameNavigator(HumanBehaviorService humanBehaviorService) {
        this.humanBehaviorService = humanBehaviorService;
    }

    /**
     * Navigate to the search form section
     * @param page Current page
     */
    public void navigateToSearchForm(Page page) {
        try {
            logger.debug("📍 Navigating to search form...");
            page.evaluate("accesoConsultaCausas()");
            humanBehaviorService.waitForDomAndNetwork(page);
            humanBehaviorService.pauseShort(page);
            logger.debug("✓ Navigated to search form");
        } catch (Exception e) {
            logger.error("❌ Error navigating to search form: ", e);
            throw new RuntimeException("Failed to navigate to search form", e);
        }
    }

    /**
     * Get the main iframe containing the search form
     * @param page Current page
     * @return Frame instance
     */
    public Frame getSearchFrame(Page page) {
        try {
            logger.debug("🔍 Getting search frame...");

            // Try multiple selectors for the iframe
            Frame frame = null;

            // Try to find frame by name
            for (Frame f : page.frames()) {
                if (f.name() != null && f.name().equals("iframeConsultaTriInicial")) {
                    frame = f;
                    break;
                }
            }

            // If not found by name, try to find by URL
            if (frame == null) {
                frame = page.frames().stream()
                    .filter(f -> f.url().contains("pjud"))
                    .findFirst()
                    .orElse(null);
            }

            if (frame == null) {
                logger.warn("⚠ Frame not found, using main page as frame");
                return null;
            }

            logger.debug("✓ Search frame obtained");
            return frame;

        } catch (Exception e) {
            logger.error("❌ Error getting search frame: ", e);
            throw new RuntimeException("Failed to get search frame", e);
        }
    }

    /**
     * Click on the "Search by Name" tab
     * @param frame Target frame
     * @param page Current page
     */
    public void clickSearchByNameTab(Frame frame, Page page) {
        try {
            logger.debug("🔗 Clicking 'Search by Name' tab...");
            frame.waitForSelector("a:has-text('Nombre')");
            frame.locator("a:has-text('Nombre')").click();
            humanBehaviorService.pauseShort(page);
            logger.debug("✓ Tab clicked");
        } catch (Exception e) {
            logger.error("❌ Error clicking search tab: ", e);
            throw new RuntimeException("Failed to click search tab", e);
        }
    }

    /**
     * Set competence to Civil (value: 3)
     * @param frame Target frame
     * @param page Current page
     */
    public void setCompetenceToCivil(Frame frame, Page page) {
        try {
            logger.debug("⚖️  Setting competence to Civil...");
            frame.waitForSelector("select[name='nomCompetencia']");
            frame.locator("select[name='nomCompetencia']").selectOption("3");
            humanBehaviorService.pauseShort(page);
            logger.debug("✓ Competence set to Civil");
        } catch (Exception e) {
            logger.error("❌ Error setting competence: ", e);
            throw new RuntimeException("Failed to set competence", e);
        }
    }
}

