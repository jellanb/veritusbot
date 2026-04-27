package com.example.veritusbot.service.scraper.browser;

import com.microsoft.playwright.Frame;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FrameNavigator {
    private static final Logger logger = LoggerFactory.getLogger(FrameNavigator.class);
    private static final String CLAVE_UNICA_MENU_SELECTOR = "a[onclick*='AutenticaCUnica']";
    private static final String CLAVE_UNICA_USER_SELECTOR = "#uname";
    private static final String CLAVE_UNICA_PASSWORD_SELECTOR = "#pword";
    private static final String CLAVE_UNICA_SUBMIT_SELECTOR = "#login-submit";
    private final HumanBehaviorService humanBehaviorService;

    @Value("${app.claveunica.run}")
    private String claveUnicaRun;

    @Value("${app.claveunica.password}")
    private String claveUnicaPassword;

    @Value("${app.scraper.claveunica.form-timeout-ms:90000}")
    private int claveUnicaFormTimeoutMs;

    public FrameNavigator(HumanBehaviorService humanBehaviorService) {
        this.humanBehaviorService = humanBehaviorService;
    }

    /**
     * Hover over "Todos los servicios" button and click "Clave Única" to authenticate
     *
     * @param page Current page
     */
    public void loginWithClaveUnica(Page page) {
        try {
            clickClaveUnicaEntry(page);
            humanBehaviorService.waitForDomAndNetwork(page);

            logger.debug("🔐 Waiting for Clave Única login form (URL: {})...", page.url());
            Frame loginFrame = waitForClaveUnicaLoginFrame(page, claveUnicaFormTimeoutMs);
            humanBehaviorService.pauseShort(page);

            logger.debug("🔐 Filling RUN field...");
            humanBehaviorService.typeFieldWithDelay(loginFrame, CLAVE_UNICA_USER_SELECTOR, claveUnicaRun);
            humanBehaviorService.pauseShort(page);

            logger.debug("🔐 Filling password field...");
            humanBehaviorService.typeFieldWithDelay(loginFrame, CLAVE_UNICA_PASSWORD_SELECTOR, claveUnicaPassword);
            humanBehaviorService.pauseShort(page);

            logger.debug("🔐 Waiting for INGRESA button to be enabled...");
            loginFrame.waitForFunction(
                "() => { const btn = document.querySelector('#login-submit'); return !!btn && !btn.disabled; }",
                null,
                new Frame.WaitForFunctionOptions().setTimeout(30000)
            );
            humanBehaviorService.pauseShort(page);

            logger.debug("🔐 Clicking INGRESA button (URL: {})...", page.url());
            loginFrame.locator(CLAVE_UNICA_SUBMIT_SELECTOR).first().click(new Locator.ClickOptions().setTimeout(15000));
            humanBehaviorService.waitForDomAndNetwork(page);
            logger.debug("🔐 Post-login current URL: {}", page.url());

            logger.debug("✓ Clave Única login submitted");
        } catch (Exception e) {
            logger.error("❌ Error during Clave Única login: ", e);
            throw new RuntimeException("Failed to login with Clave Única", e);
        }
    }

    private void clickClaveUnicaEntry(Page page) {
        RuntimeException lastError = null;

        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                logger.debug("🔐 Hovering over 'Todos los servicios' button (attempt {}/2)...", attempt);
                page.hover("button.dropbtn");
                humanBehaviorService.pauseShort(page);

                page.waitForSelector(
                        CLAVE_UNICA_MENU_SELECTOR,
                        new Page.WaitForSelectorOptions()
                                .setState(WaitForSelectorState.VISIBLE)
                                .setTimeout(15000)
                );
                page.locator(CLAVE_UNICA_MENU_SELECTOR).first().click(new Locator.ClickOptions().setTimeout(15000));
                return;
            } catch (Exception e) {
                lastError = new RuntimeException("Failed to click Clave Única entry", e);
                logger.warn("⚠️ Could not click Clave Única entry on attempt {}: {}", attempt, e.getMessage());
                humanBehaviorService.pauseShort(page);
            }
        }

        throw lastError != null ? lastError : new RuntimeException("Failed to click Clave Única entry");
    }

    private Frame waitForClaveUnicaLoginFrame(Page page, int timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        int attempts = 0;

        while (System.currentTimeMillis() < deadline) {
            attempts++;

            try {
                page.waitForSelector(
                        CLAVE_UNICA_USER_SELECTOR,
                        new Page.WaitForSelectorOptions()
                                .setState(WaitForSelectorState.VISIBLE)
                                .setTimeout(1500)
                );
                logger.debug("🔐 Clave Única form found in main frame");
                return page.mainFrame();
            } catch (Exception ignored) {
                // Keep probing child frames.
            }

            for (Frame frame : page.frames()) {
                if (frame == page.mainFrame()) {
                    continue;
                }

                try {
                    frame.waitForSelector(
                            CLAVE_UNICA_USER_SELECTOR,
                            new Frame.WaitForSelectorOptions()
                                    .setState(WaitForSelectorState.VISIBLE)
                                    .setTimeout(800)
                    );
                    logger.debug("🔐 Clave Única form found in frame name='{}' url='{}'", frame.name(), frame.url());
                    return frame;
                } catch (Exception ignored) {
                    // Probe next frame.
                }
            }

            if (attempts % 5 == 0) {
                logger.debug("🔐 Waiting for form... attempt={} | url={} | title={} | frames={}",
                        attempts, page.url(), page.title(), page.frames().size());
            }

            page.waitForTimeout(500);
        }

        throw new RuntimeException("Clave Única form did not appear within " + timeoutMs + "ms");
    }

    /**
     * Open the "Consulta Unificada" section from the left-side menu.
     *
     * <p>This replaces the previous direct call to {@code accesoConsultaCausas()}.
     * The target element is the anchor rendered in the left menu:
     * <pre>{@code <a href="#" onclick="consultaUnificada();">...Consulta Unificada</a>}</pre>
     *
     * <p>We click the anchor (instead of evaluating the JS function directly) to
     * keep the interaction human-like and to trigger the native onclick handler
     * registered by the page.
     *
     * @param page Current page (already authenticated via Clave Única)
     */
    public void openConsultaUnificada(Page page) {
        try {
            logger.debug("📍 Opening 'Consulta Unificada' from left menu...");
            humanBehaviorService.pauseInteraction(page);

            // Wait for the anchor in the left menu to be attached to the DOM
            String consultaUnificadaSelector = "a[onclick*='consultaUnificada']";
            try {
                page.waitForSelector(
                        consultaUnificadaSelector,
                        new Page.WaitForSelectorOptions().setTimeout(90000)
                );
            } catch (Exception waitEx) {
                logger.error("❌ 'Consulta Unificada' link not available. Current URL: {}, Title: {}",
                        page.url(), page.title());
                throw waitEx;
            }

            // Click the link — onclick="consultaUnificada();" will fire naturally
            page.click(consultaUnificadaSelector);
            humanBehaviorService.waitForDomAndNetwork(page);
            humanBehaviorService.pauseShort(page);

            logger.debug("✓ 'Consulta Unificada' opened");
        } catch (Exception e) {
            logger.error("❌ Error opening 'Consulta Unificada': ", e);
            throw new RuntimeException("Failed to open 'Consulta Unificada'", e);
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

