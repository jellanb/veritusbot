package com.example.veritusbot.service.scraper.form;

import com.microsoft.playwright.Frame;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.PlaywrightException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class TribunalSelector {
    private static final Logger logger = LoggerFactory.getLogger(TribunalSelector.class);

    /**
     * Open the tribunal dropdown menu
     * @param frame Target frame
     */
    public void openDropdown(Frame frame) {
        try {
            logger.debug("📂 Opening tribunal dropdown...");
            Locator button = frame.locator("button[data-toggle='dropdown']");
            if (button.count() > 0) {
                button.first().click();
                frame.waitForSelector("ul.dropdown-menu.inner", new Frame.WaitForSelectorOptions().setTimeout(5000));
                logger.debug("✓ Dropdown opened successfully");
            } else {
                logger.warn("⚠ Dropdown button not found");
            }
        } catch (Exception e) {
            logger.error("❌ Error opening dropdown: ", e);
            throw new RuntimeException("Failed to open dropdown", e);
        }
    }

    /**
     * Load all tribunals from the dropdown
     * @param frame Target frame
     * @return Map of tribunal name to index
     */
    public Map<String, Integer> loadAllTribunals(Frame frame) {
        try {
            logger.debug("📋 Loading all tribunals...");

            Object result = frame.evaluate("""
                () => {
                    const items = document.querySelectorAll('ul.dropdown-menu.inner li[data-original-index]');
                    const map = {};
                    items.forEach(item => {
                        const index = item.getAttribute('data-original-index');
                        const text = item.querySelector('span.text')?.textContent?.trim() || '';
                        if (text) {
                            map[text] = parseInt(index);
                        }
                    });
                    return map;
                }
            """);

            if (result instanceof Map) {
                Map<String, Integer> tribunals = (Map<String, Integer>) result;
                logger.debug("✓ Loaded {} tribunals", tribunals.size());
                return tribunals;
            }

            logger.warn("⚠ Failed to load tribunals, returning empty map");
            return new LinkedHashMap<>();

        } catch (Exception e) {
            logger.error("❌ Error loading tribunals: ", e);
            return new LinkedHashMap<>();
        }
    }

    /**
     * Select a tribunal by its index
     * @param frame Target frame
     * @param tribunalName Tribunal name
     * @param index Tribunal index
     */
    public void selectTribunal(Frame frame, String tribunalName, int index) {
        try {
            // SKIP placeholder
            if (tribunalName.contains("Select") || tribunalName.trim().isEmpty()) {
                logger.debug("⏭️  Skipping placeholder: {}", tribunalName);
                return;
            }

            String selector = String.format("li[data-original-index=\"%d\"] a", index);
            logger.debug("🖱️  Selecting tribunal: {} (index: {})", tribunalName, index);

            Locator element = frame.locator(selector);
            if (element.count() > 0) {
                element.first().waitFor(new Locator.WaitForOptions().setTimeout(5000));
                element.first().scrollIntoViewIfNeeded();
                Thread.sleep(500);
                element.first().click();
                logger.debug("✓ Tribunal selected successfully");
            } else {
                logger.warn("⚠ Tribunal element not found: {}", tribunalName);
            }

        } catch (Exception e) {
            logger.error("❌ Error selecting tribunal: ", e);
            throw new RuntimeException("Failed to select tribunal: " + tribunalName, e);
        }
    }

    /**
     * Close the dropdown menu
     * @param frame Target frame
     */
    public void closeDropdown(Frame frame) {
        try {
            logger.debug("📂 Closing dropdown...");
            frame.evaluate("""
                (function() {
                    const dropdowns = document.querySelectorAll('.dropdown-menu');
                    dropdowns.forEach(el => {
                        el.classList.remove('show', 'open');
                    });
                })()
            """);
            logger.debug("✓ Dropdown closed");
        } catch (Exception e) {
            logger.warn("⚠ Error closing dropdown: {}", e.getMessage());
        }
    }
}

