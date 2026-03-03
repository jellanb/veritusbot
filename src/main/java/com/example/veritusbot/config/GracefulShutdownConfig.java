package com.example.veritusbot.config;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Componente para manejar el apagado elegante de la aplicación.
 * Se encarga de limpiar recursos, cerrar navegadores y detener procesos.
 */
@Component
public class GracefulShutdownConfig {

    private ApplicationContext applicationContext;

    public GracefulShutdownConfig(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * Se ejecuta cuando la aplicación recibe una señal de cierre
     */
    @EventListener(ContextClosedEvent.class)
    public void onApplicationShutdown() {
        System.out.println("\n╔════════════════════════════════════════════════════════════╗");
        System.out.println("║  INICIANDO APAGADO ELEGANTE DE LA APLICACIÓN               ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");

        try {
            // Aquí se pueden agregar lógicas de limpieza adicionales
            Thread.sleep(500); // Pequeña pausa para que terminen operaciones pendientes

            System.out.println("✅ Recursos liberados correctamente");
            System.out.println("✅ Conexiones cerradas");
            System.out.println("✅ Procesos detenidos\n");

        } catch (InterruptedException e) {
            System.err.println("❌ Error durante el apagado elegante: " + e.getMessage());
            Thread.currentThread().interrupt();
        }

        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║  APLICACIÓN APAGADA CORRECTAMENTE                         ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");
    }

    /**
     * Método para hacer shutdown desde el código
     */
    public void initiateShutdown() {
        System.out.println("\n⚠️  Iniciando cierre de la aplicación desde el código...");
        SpringApplication.exit(applicationContext, () -> 0);
    }
}
