package com.example.veritusbot.service.scraper.form;

import com.microsoft.playwright.Frame;
import com.microsoft.playwright.Locator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FormFiller {
    private static final Logger logger = LoggerFactory.getLogger(FormFiller.class);

    /**
     * Fill the search form with person data
     * @param frame Target frame containing the form
     * @param names Person names
     * @param year Year to search
     */
    public void fillSearchForm(Frame frame, String names, int year) {
        try {
            logger.debug("📝 Filling search form for: {} (year: {})", names, year);

            frame.fill("input[name='nomNombre']", names);
            frame.fill("input[id='nomEra']", String.valueOf(year));

            logger.debug("✓ Form filled successfully");
        } catch (Exception e) {
            logger.error("❌ Error filling search form: ", e);
            throw new RuntimeException("Failed to fill search form", e);
        }
    }

    /**
     * Fill court/tribunal field
     * @param frame Target frame
     * @param tribunalName Tribunal name
     */
    public void fillTribunal(Frame frame, String tribunalName) {
        try {
            logger.debug("📝 Filling tribunal field: {}", tribunalName);
            frame.fill("input[name='nomTribunal']", tribunalName);
            logger.debug("✓ Tribunal field filled");
        } catch (Exception e) {
            logger.error("❌ Error filling tribunal field: ", e);
            throw new RuntimeException("Failed to fill tribunal field", e);
        }
    }

    /**
     * Submit the search form
     * @param frame Target frame
     */
    public void submitForm(Frame frame) {
        try {
            logger.debug("🔍 Submitting search form...");
            Locator submitButton = frame.locator("button[type='submit']");
            if (submitButton.count() > 0) {
                submitButton.click();
                logger.debug("✓ Form submitted successfully");
            } else {
                logger.warn("⚠ Submit button not found");
            }
        } catch (Exception e) {
            logger.error("❌ Error submitting form: ", e);
            throw new RuntimeException("Failed to submit form", e);
        }
    }

    /**
     * Select an option from competence dropdown
     * @param frame Target frame
     * @param optionValue Option value to select
     */
    public void selectCompetence(Frame frame, String optionValue) {
        try {
            logger.debug("📝 Selecting competence: {}", optionValue);
            frame.locator("select[name='nomCompetencia']").selectOption(optionValue);
            logger.debug("✓ Competence selected");
        } catch (Exception e) {
            logger.error("❌ Error selecting competence: ", e);
            throw new RuntimeException("Failed to select competence", e);
        }
    }
}

