package com.example.veritusbot.service.scraper.browser;

import com.example.veritusbot.service.scraper.config.ScraperConfig;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class HumanBehaviorService {
    private static final Logger logger = LoggerFactory.getLogger(HumanBehaviorService.class);

    public void pauseShort(Page page) {
        pause(page, ScraperConfig.HUMAN_PAUSE_MIN_MS, ScraperConfig.HUMAN_PAUSE_MAX_MS);
    }

    public void pauseInteraction(Page page) {
        logger.debug("⏸️  Interaction pause: {}ms", ScraperConfig.HUMAN_INTERACTION_EXTRA_MS);
        page.waitForTimeout(ScraperConfig.HUMAN_INTERACTION_EXTRA_MS);
    }

    public void pause(Page page, int minMs, int maxMs) {
        int delay = ThreadLocalRandom.current().nextInt(minMs, Math.max(minMs + 1, maxMs + 1));
        logger.debug("⏳ Human pause: {}ms (range {}-{})", delay, minMs, maxMs);
        page.waitForTimeout(delay);
    }

    public void waitForDomAndNetwork(Page page) {
        logger.debug("🌐 Waiting for DOMCONTENTLOADED + NETWORKIDLE");
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        page.waitForLoadState(LoadState.NETWORKIDLE);
        page.locator("body").first().waitFor(new Locator.WaitForOptions().setTimeout(10000));
        logger.debug("✅ DOM and network ready");
    }

    public void gradualScroll(Page page) {
        logger.debug("🖱️  Starting gradual scroll (steps={})", ScraperConfig.HUMAN_SCROLL_STEPS);
        for (int i = 0; i < ScraperConfig.HUMAN_SCROLL_STEPS; i++) {
            int pixels = ThreadLocalRandom.current().nextInt(120, 320);
            try {
                // Use explicit lambda parameter to avoid ReferenceError: arguments is not defined.
                page.evaluate("(y) => window.scrollBy(0, y)", pixels);
                logger.debug("↕️  Scroll step {}/{} by {}px", i + 1, ScraperConfig.HUMAN_SCROLL_STEPS, pixels);
            } catch (Exception e) {
                logger.debug("Non-critical scroll step failed: {}", e.getMessage());
            }
            pauseShort(page);
        }

        try {
            page.evaluate("() => window.scrollTo(0, 0)");
        } catch (Exception e) {
            logger.debug("Could not reset scroll position: {}", e.getMessage());
        }

        logger.debug("🏁 Gradual scroll finished");
    }
}

