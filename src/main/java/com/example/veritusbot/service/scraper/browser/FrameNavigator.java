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
     *
     * @param page Current page
     */
    public void navigateToSearchForm(Page page) {
        try {
            logger.debug("📍 Navigating to search form...");
            humanBehaviorService.pauseInteraction(page);
            try {
                page.waitForFunction(
                        "typeof accesoConsultaCausas === 'function'",
                        null,
                        new Page.WaitForFunctionOptions().setTimeout(90000)
                );
            } catch (Exception waitEx) {
                logger.error("❌ accesoConsultaCausas not available. Current URL: {}, Title: {}",
                        page.url(), page.title());
                throw waitEx;
            }
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
     *
     * @param page Current page
     * @return Frame instance
     */
    public Frame getSearchFrame(Page page) {
        try {
            logger.debug("🔍 Getting search frame...");
            logger.debug("🧱 Total frames available: {}", page.frames().size());

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
                logger.debug("🔎 Frame by name not found, trying URL strategy");
                frame = page.frames().stream()
                        .filter(f -> f.url().contains("pjud"))
                        .findFirst()
                        .orElse(null);
            }

            if (frame == null) {
                logger.warn("⚠ Frame not found, using main page as frame");
                return null;
            }

            logger.debug("✓ Search frame obtained (name='{}', url='{}')", frame.name(), frame.url());
            return frame;

        } catch (Exception e) {
            logger.error("❌ Error getting search frame: ", e);
            throw new RuntimeException("Failed to get search frame", e);
        }
    }

    /**
     * Click on the "Search by Name" tab
     *
     * @param frame Target frame
     * @param page  Current page
     */
    public void clickSearchByNameTab(Frame frame, Page page) {
        try {
            logger.debug("🔗 Clicking 'Search by Name' tab...");
            logger.debug("🔗 Waiting selector a:has-text('Nombre')");
            frame.waitForSelector(
                    "a:has-text('Nombre')",
                    new Frame.WaitForSelectorOptions().setTimeout(120000)
            );
            humanBehaviorService.pauseInteraction(page);
            humanBehaviorService.pauseInteraction(page);
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
     *
     * @param frame Target frame
     * @param page  Current page
     */
    public void setCompetenceToCivil(Frame frame, Page page) {
        try {
            logger.debug("⚖️  Setting competence to Civil...");
            logger.debug("⚖️  Waiting selector select[name='nomCompetencia'] and setting value=3");
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

