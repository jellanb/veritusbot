package com.example.veritusbot.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Sistema centralizado de logging con niveles (INFO, WARNING, ERROR, DEBUG)
 * Configurable según el entorno: desarrollo, producción, pruebas
 */
@Component
public class LoggerUtil {

    public enum LogLevel {
        DEBUG(0, "🔍 DEBUG"),
        INFO(1, "ℹ️  INFO"),
        WARNING(2, "⚠️  WARNING"),
        ERROR(3, "❌ ERROR");

        private final int level;
        private final String prefix;

        LogLevel(int level, String prefix) {
            this.level = level;
            this.prefix = prefix;
        }

        public int getLevel() {
            return level;
        }

        public String getPrefix() {
            return prefix;
        }
    }

    public enum Environment {
        DESARROLLO("desarrollo"),
        PRODUCCION("produccion"),
        PRUEBAS("pruebas");

        private final String value;

        Environment(String value) {
            this.value = value;
        }

        public static Environment fromString(String value) {
            for (Environment env : Environment.values()) {
                if (env.value.equalsIgnoreCase(value)) {
                    return env;
                }
            }
            return DESARROLLO; // Default
        }

        public String getValue() {
            return value;
        }
    }

    @Value("${app.environment:desarrollo}")
    private String appEnvironment;

    /**
     * Obtiene el nivel mínimo de log según el entorno
     * - DESARROLLO: DEBUG (todos los logs)
     * - PRUEBAS: INFO (info, warning, error)
     * - PRODUCCION: WARNING (solo warning y error)
     */
    private LogLevel getMinimumLogLevel() {
        Environment env = Environment.fromString(appEnvironment);

        switch (env) {
            case DESARROLLO:
                return LogLevel.DEBUG;
            case PRUEBAS:
                return LogLevel.INFO;
            case PRODUCCION:
                return LogLevel.WARNING;
            default:
                return LogLevel.DEBUG;
        }
    }

    /**
     * Verifica si un nivel de log debe ser mostrado
     */
    private boolean shouldLog(LogLevel level) {
        return level.getLevel() >= getMinimumLogLevel().getLevel();
    }

    /**
     * Log de DEBUG (solo en desarrollo)
     */
    public void debug(String message) {
        if (shouldLog(LogLevel.DEBUG)) {
            System.out.println(LogLevel.DEBUG.getPrefix() + " " + message);
        }
    }

    /**
     * Log de DEBUG con formato
     */
    public void debug(String message, Object... args) {
        if (shouldLog(LogLevel.DEBUG)) {
            String formattedMessage = String.format(message, args);
            System.out.println(LogLevel.DEBUG.getPrefix() + " " + formattedMessage);
        }
    }

    /**
     * Log de DEBUG con excepción
     */
    public void debug(String message, Throwable throwable) {
        if (shouldLog(LogLevel.DEBUG)) {
            System.out.println(LogLevel.DEBUG.getPrefix() + " " + message);
            throwable.printStackTrace();
        }
    }

    /**
     * Log de INFO (desarrollo, pruebas)
     */
    public void info(String message) {
        if (shouldLog(LogLevel.INFO)) {
            System.out.println(LogLevel.INFO.getPrefix() + " " + message);
        }
    }

    /**
     * Log de INFO con formato (usando placeholders)
     */
    public void info(String message, Object... args) {
        if (shouldLog(LogLevel.INFO)) {
            String formattedMessage = String.format(message, args);
            System.out.println(LogLevel.INFO.getPrefix() + " " + formattedMessage);
        }
    }

    /**
     * Log de WARNING (todos los entornos)
     */
    public void warning(String message) {
        if (shouldLog(LogLevel.WARNING)) {
            System.out.println(LogLevel.WARNING.getPrefix() + " " + message);
        }
    }

    /**
     * Log de WARNING con formato
     */
    public void warning(String message, Object... args) {
        if (shouldLog(LogLevel.WARNING)) {
            String formattedMessage = String.format(message, args);
            System.out.println(LogLevel.WARNING.getPrefix() + " " + formattedMessage);
        }
    }

    /**
     * Log de WARNING con excepción
     */
    public void warning(String message, Throwable throwable) {
        if (shouldLog(LogLevel.WARNING)) {
            System.out.println(LogLevel.WARNING.getPrefix() + " " + message);
            throwable.printStackTrace();
        }
    }

    /**
     * Log de ERROR (todos los entornos)
     */
    public void error(String message) {
        if (shouldLog(LogLevel.ERROR)) {
            System.err.println(LogLevel.ERROR.getPrefix() + " " + message);
        }
    }

    /**
     * Log de ERROR con formato
     */
    public void error(String message, Object... args) {
        if (shouldLog(LogLevel.ERROR)) {
            String formattedMessage = String.format(message, args);
            System.err.println(LogLevel.ERROR.getPrefix() + " " + formattedMessage);
        }
    }

    /**
     * Log de ERROR con excepción
     */
    public void error(String message, Throwable throwable) {
        if (shouldLog(LogLevel.ERROR)) {
            System.err.println(LogLevel.ERROR.getPrefix() + " " + message);
            throwable.printStackTrace();
        }
    }

    /**
     * Log con separador visual (para secciones)
     */
    public void section(String sectionName) {
        if (shouldLog(LogLevel.INFO)) {
            System.out.println("\n╔════════════════════════════════════════════════════════════╗");
            System.out.println("║  " + sectionName);
            System.out.println("╚════════════════════════════════════════════════════════════╝\n");
        }
    }

    /**
     * Log con línea separadora
     */
    public void separator() {
        if (shouldLog(LogLevel.DEBUG)) {
            System.out.println("═".repeat(60));
        }
    }

    /**
     * Obtiene el entorno actual
     */
    public Environment getCurrentEnvironment() {
        return Environment.fromString(appEnvironment);
    }

    /**
     * Log de información de startup
     */
    public void logStartupInfo() {
        info("═══════════════════════════════════════════════════════════");
        info("VERITUS BOT INICIANDO");
        info("Entorno: " + getCurrentEnvironment().getValue());
        info("Nivel de logs: " + getMinimumLogLevel().name());
        info("═══════════════════════════════════════════════════════════");
    }
}

