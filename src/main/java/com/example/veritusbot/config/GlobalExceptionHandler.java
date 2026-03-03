package com.example.veritusbot.config;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

/**
 * Manejador global de excepciones que garantiza un graceful shutdown
 * en caso de errores críticos
 */
@ControllerAdvice
@Component
public class GlobalExceptionHandler {

    private final ApplicationContext applicationContext;
    private final ResourceCleanupManager resourceCleanupManager;

    public GlobalExceptionHandler(ApplicationContext applicationContext,
                                 ResourceCleanupManager resourceCleanupManager) {
        this.applicationContext = applicationContext;
        this.resourceCleanupManager = resourceCleanupManager;
    }

    /**
     * Maneja excepciones genéricas
     */
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public Map<String, Object> handleGeneralException(Exception ex) {
        System.err.println("\n╔════════════════════════════════════════════════════════════╗");
        System.err.println("║  ERROR CRÍTICO DETECTADO                                   ║");
        System.err.println("╚════════════════════════════════════════════════════════════╝\n");

        System.err.println("❌ Excepción: " + ex.getClass().getSimpleName());
        System.err.println("📝 Mensaje: " + ex.getMessage());
        System.err.println("\n📊 Estado de recursos: " + resourceCleanupManager.getResourceStats());

        ex.printStackTrace();

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", ex.getMessage());
        errorResponse.put("timestamp", System.currentTimeMillis());

        return errorResponse;
    }

    /**
     * Maneja RuntimeException
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseBody
    public Map<String, Object> handleRuntimeException(RuntimeException ex) {
        System.err.println("\n╔════════════════════════════════════════════════════════════╗");
        System.err.println("║  ERROR EN TIEMPO DE EJECUCIÓN                              ║");
        System.err.println("╚════════════════════════════════════════════════════════════╝\n");

        System.err.println("❌ RuntimeException: " + ex.getMessage());
        System.err.println("📊 Estado de recursos: " + resourceCleanupManager.getResourceStats());

        ex.printStackTrace();

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Error en tiempo de ejecución");
        errorResponse.put("details", ex.getMessage());
        errorResponse.put("timestamp", System.currentTimeMillis());

        return errorResponse;
    }
}
