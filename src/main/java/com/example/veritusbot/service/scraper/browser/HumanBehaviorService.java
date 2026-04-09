package com.example.veritusbot.service.scraper.browser;

import com.example.veritusbot.service.scraper.config.ScraperConfig;
import com.microsoft.playwright.Frame;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.LoadState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class HumanBehaviorService {
    private static final Logger logger = LoggerFactory.getLogger(HumanBehaviorService.class);
    private static final double NETWORK_IDLE_BEST_EFFORT_TIMEOUT_MS = 5000;

    // 5% chance of a long "distracted" pause (4–12 seconds) on each pauseShort call
    private static final double DISTRACTION_PROBABILITY = 0.05;
    private static final int DISTRACTION_MIN_MS = 4000;
    private static final int DISTRACTION_MAX_MS = 12000;

    public void pauseShort(Page page) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();

        // Occasionally simulate the user getting distracted
        if (rng.nextDouble() < DISTRACTION_PROBABILITY) {
            int distractionDelay = rng.nextInt(DISTRACTION_MIN_MS, DISTRACTION_MAX_MS + 1);
            logger.debug("⏸️  Distraction pause: {}ms", distractionDelay);
            page.waitForTimeout(distractionDelay);
            return;
        }

        // Non-uniform distribution: bias toward shorter pauses but allow occasional longer ones
        // ~70% fast (min..mid), ~30% slow (mid..max)
        int mid = (ScraperConfig.HUMAN_PAUSE_MIN_MS + ScraperConfig.HUMAN_PAUSE_MAX_MS) / 2;
        int delay;
        if (rng.nextDouble() < 0.70) {
            delay = rng.nextInt(ScraperConfig.HUMAN_PAUSE_MIN_MS, Math.max(ScraperConfig.HUMAN_PAUSE_MIN_MS + 1, mid + 1));
        } else {
            delay = rng.nextInt(mid, Math.max(mid + 1, ScraperConfig.HUMAN_PAUSE_MAX_MS + 1));
        }
        logger.debug("⏳ Human pause: {}ms (range {}-{})", delay, ScraperConfig.HUMAN_PAUSE_MIN_MS, ScraperConfig.HUMAN_PAUSE_MAX_MS);
        page.waitForTimeout(delay);
    }

    public void pauseInteraction(Page page) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        // Add ±30% jitter to interaction pause to avoid fixed-interval fingerprinting
        int base = ScraperConfig.HUMAN_INTERACTION_EXTRA_MS;
        int jitter = (int) (base * 0.30);
        int delay = base + rng.nextInt(-jitter, jitter + 1);
        logger.debug("⏸️  Interaction pause: {}ms", delay);
        page.waitForTimeout(delay);
    }

    public void pause(Page page, int minMs, int maxMs) {
        int delay = ThreadLocalRandom.current().nextInt(minMs, Math.max(minMs + 1, maxMs + 1));
        logger.debug("⏳ Human pause: {}ms (range {}-{})", delay, minMs, maxMs);
        page.waitForTimeout(delay);
    }

    public void waitForDomAndNetwork(Page page) {
        logger.debug("🌐 Waiting for DOM readiness + stable body element");
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        page.locator("body").first().waitFor(new Locator.WaitForOptions().setTimeout(10000));

        // Best-effort wait: some sites keep long-lived network requests and never reach networkidle.
        try {
            page.waitForLoadState(
                    LoadState.NETWORKIDLE,
                    new Page.WaitForLoadStateOptions().setTimeout(NETWORK_IDLE_BEST_EFFORT_TIMEOUT_MS)
            );
            logger.debug("✅ DOM, body and network idle ready");
        } catch (PlaywrightException e) {
            logger.debug("ℹ️ NETWORKIDLE not reached within {}ms, continuing: {}",
                    (long) NETWORK_IDLE_BEST_EFFORT_TIMEOUT_MS,
                    e.getMessage());
            logger.debug("✅ DOM and body ready (network idle skipped)");
        }
    }

    /**
     * Type text into a form field character by character with random delays to simulate human typing.
     * @param frame Target frame containing the field
     * @param selector CSS selector for the input field
     * @param text Text to type
     */
    public void typeFieldWithDelay(Frame frame, String selector, String text) {
        logger.debug("⌨️  Typing into '{}' with human delays ({} chars)", selector, text.length());
        frame.locator(selector).click();
        frame.locator(selector).fill("");
        for (char c : text.toCharArray()) {
            frame.locator(selector).type(String.valueOf(c));
            int delay = ThreadLocalRandom.current().nextInt(60, 201);
            frame.page().waitForTimeout(delay);
        }
        logger.debug("⌨️  Finished typing into '{}'", selector);
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
