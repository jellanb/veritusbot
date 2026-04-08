package com.example.veritusbot.service.scraper.form;

import com.example.veritusbot.dto.PersonaDTO;
import com.example.veritusbot.service.scraper.browser.HumanBehaviorService;
import com.example.veritusbot.service.scraper.config.ScraperConfig;
import com.microsoft.playwright.Frame;
import com.microsoft.playwright.Locator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FormFiller {
    private static final Logger logger = LoggerFactory.getLogger(FormFiller.class);
    private final HumanBehaviorService humanBehaviorService;

    public FormFiller(HumanBehaviorService humanBehaviorService) {
        this.humanBehaviorService = humanBehaviorService;
    }

    /**
     * Fill the search form with person data
     * @param frame Target frame containing the form
     * @param person Person to search
     * @param year Year to search
     */
    public void fillSearchForm(Frame frame, PersonaDTO person, int year) {
        try {
            logger.debug("📝 Filling search form for: {} (year: {})", person.getNombres(), year);

            humanBehaviorService.pauseInteraction(frame.page());
            humanBehaviorService.typeFieldWithDelay(frame, "input[name='nomNombre']", person.getNombres());
            logger.debug("📝 Field nomNombre filled");
            humanBehaviorService.pauseInteraction(frame.page());
            humanBehaviorService.typeFieldWithDelay(frame, "input[name='nomApePaterno']", person.getApellidoPaterno());
            logger.debug("📝 Field nomApePaterno filled");
            humanBehaviorService.pauseInteraction(frame.page());
            humanBehaviorService.typeFieldWithDelay(frame, "input[name='nomApeMaterno']", person.getApellidoMaterno());
            logger.debug("📝 Field nomApeMaterno filled");
            humanBehaviorService.pauseInteraction(frame.page());
            humanBehaviorService.typeFieldWithDelay(frame, "input[id='nomEra']", String.valueOf(year));
            logger.debug("📝 Field nomEra filled with year {}", year);

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
            humanBehaviorService.pauseInteraction(frame.page());
            humanBehaviorService.typeFieldWithDelay(frame, "input[name='nomTribunal']", tribunalName);
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
            Locator submitButton = frame.locator(ScraperConfig.SUBMIT_BUTTON_SELECTOR);
            logger.debug("🔍 Submit button count: {}", submitButton.count());
            if (submitButton.count() > 0) {
                humanBehaviorService.pauseInteraction(frame.page());
                submitButton.click();
                logger.debug("✓ Form submitted successfully");
            } else {
                logger.warn("⚠ Submit button not found using selector {}", ScraperConfig.SUBMIT_BUTTON_SELECTOR);
            }
        } catch (Exception e) {
            logger.error("❌ Error submitting form with selector {}: ", ScraperConfig.SUBMIT_BUTTON_SELECTOR, e);
            throw new RuntimeException("Failed to submit form using selector " + ScraperConfig.SUBMIT_BUTTON_SELECTOR, e);
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
            humanBehaviorService.pauseInteraction(frame.page());
            frame.locator("select[name='nomCompetencia']").selectOption(optionValue);
            logger.debug("✓ Competence selected");
        } catch (Exception e) {
            logger.error("❌ Error selecting competence: ", e);
            throw new RuntimeException("Failed to select competence", e);
        }
    }
}

