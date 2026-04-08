package com.example.veritusbot.service.scraper.config;

/**
 * Configuration class for scraper settings
 */
public class ScraperConfig {
    // Timeouts
    public static final int DEFAULT_TIMEOUT = 30000;  // 30 seconds
    public static final int NAVIGATION_TIMEOUT = 30000;
    public static final int WAIT_FOR_RESULTS_TIMEOUT = 15000;  // 15 seconds

    // Delays
    public static final long DELAY_BETWEEN_SEARCHES = 2000;  // 2 seconds
    public static final long DELAY_BETWEEN_YEARS = 5000;  // 5 seconds

    // URLs
    public static final String PJUD_HOME_URL = "https://oficinajudicialvirtual.pjud.cl/home/index.php";

    // Browser fingerprint aligned with Chilean locale (es-CL)
    public static final String BROWSER_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36";
    public static final String BROWSER_ACCEPT = "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7";
    public static final String BROWSER_ACCEPT_LANGUAGE = "es-CL,es;q=0.9";
    public static final String BROWSER_LOCALE = "es-CL";
    public static final String BROWSER_TIMEZONE = "America/Toronto";
    public static final String BROWSER_PLATFORM = "Win32";
    public static final String SEC_CH_UA = "\"Chromium\";v=\"136\", \"Google Chrome\";v=\"136\", \"Not-A.Brand\";v=\"99\"";
    public static final String SEC_CH_UA_PLATFORM = "\"Windows\"";
    public static final int BROWSER_VIEWPORT_WIDTH = 1366;
    public static final int BROWSER_VIEWPORT_HEIGHT = 768;

    // Session persistence
    public static final String SESSION_STATE_DIR = "target/playwright-sessions";

    // Human-like timings
    public static final int HUMAN_PAUSE_MIN_MS = 220;
    public static final int HUMAN_PAUSE_MAX_MS = 780;
    public static final int HUMAN_INTERACTION_EXTRA_MS = 3000;
    public static final int HUMAN_SCROLL_STEPS = 4;

    // Selectors
    public static final String DROPDOWN_SELECTOR = "button[data-toggle='dropdown']";
    public static final String TRIBUNAL_DROPDOWN_MENU = "ul.dropdown-menu.inner";
    public static final String SUBMIT_BUTTON_SELECTOR = "button[type='submit']#btnConConsultaNom";

    // Form fields
    public static final String INPUT_NAMES_SELECTOR = "input[name='nomNombre']";
    public static final String INPUT_YEAR_SELECTOR = "input[id='nomEra']";
    public static final String SELECT_COMPETENCE_SELECTOR = "select[name='nomCompetencia']";

    // Competence option
    public static final String COMPETENCE_CIVIL = "3";

    // Results table
    public static final String RESULTS_TABLE_SELECTOR = "table#dtaTableDetalleNombre";
    public static final String RESULTS_TBODY_SELECTOR = "tbody#verDetalleNombre";

    // Search phases
    public static final int PHASE_1_TRIBUNALS = 30;  // Santiago tribunals
    public static final int PHASE_2_TRIBUNALS = 201; // Other tribunals

    // Retry settings
    public static final int MAX_RETRIES = 3;
    public static final long RETRY_DELAY = 1000;  // 1 second

    private ScraperConfig() {
        // Private constructor to prevent instantiation
    }
}

