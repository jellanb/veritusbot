package com.example.veritusbot.config;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Componente para gestionar recursos de Playwright y Threads de forma segura.
 * Garantiza que los navegadores se cierren correctamente al apagar la aplicación.
 */
@Component
public class ResourceCleanupManager {

    private final List<Browser> openBrowsers = new CopyOnWriteArrayList<>();
    private final List<BrowserContext> openContexts = new CopyOnWriteArrayList<>();
    private final List<Page> openPages = new CopyOnWriteArrayList<>();
    private final List<ExecutorService> executorServices = new CopyOnWriteArrayList<>();
    private final List<Playwright> playwrights = new CopyOnWriteArrayList<>();

    /**
     * Registra un browser abierto para posterior limpieza
     */
    public void registerBrowser(Browser browser) {
        if (browser != null) {
            openBrowsers.add(browser);
        }
    }

    /**
     * Registra un contexto abierto para posterior limpieza
     */
    public void registerBrowserContext(BrowserContext context) {
        if (context != null) {
            openContexts.add(context);
        }
    }

    /**
     * Registra una página abierta para posterior limpieza
     */
    public void registerPage(Page page) {
        if (page != null) {
            openPages.add(page);
        }
    }

    /**
     * Registra un ExecutorService para posterior shutdown
     */
    public void registerExecutorService(ExecutorService service) {
        if (service != null) {
            executorServices.add(service);
        }
    }

    /**
     * Registra una instancia de Playwright para posterior cierre
     */
    public void registerPlaywright(Playwright playwright) {
        if (playwright != null) {
            playwrights.add(playwright);
        }
    }

    /**
     * Desregistra un browser después de cerrarlo
     */
    public void unregisterBrowser(Browser browser) {
        openBrowsers.remove(browser);
    }

    /**
     * Desregistra un contexto después de cerrarlo
     */
    public void unregisterBrowserContext(BrowserContext context) {
        openContexts.remove(context);
    }

    /**
     * Desregistra una página después de cerrarla
     */
    public void unregisterPage(Page page) {
        openPages.remove(page);
    }

    /**
     * Se ejecuta automáticamente cuando la aplicación se apaga
     * Cierra todos los recursos de forma segura
     */
    @PreDestroy
    public void cleanupAllResources() {
        System.out.println("\n🧹 Iniciando limpieza de recursos de Playwright...\n");

        // Cerrar todas las páginas
        if (!openPages.isEmpty()) {
            System.out.println("📄 Cerrando " + openPages.size() + " página(s)...");
            for (Page page : new ArrayList<>(openPages)) {
                try {
                    page.close();
                    System.out.println("   ✓ Página cerrada");
                } catch (Exception e) {
                    System.err.println("   ✗ Error cerrando página: " + e.getMessage());
                }
            }
            openPages.clear();
        }

        // Cerrar todos los contextos
        if (!openContexts.isEmpty()) {
            System.out.println("🔐 Cerrando " + openContexts.size() + " contexto(s)...");
            for (BrowserContext context : new ArrayList<>(openContexts)) {
                try {
                    context.close();
                    System.out.println("   ✓ Contexto cerrado");
                } catch (Exception e) {
                    System.err.println("   ✗ Error cerrando contexto: " + e.getMessage());
                }
            }
            openContexts.clear();
        }

        // Cerrar todos los navegadores
        if (!openBrowsers.isEmpty()) {
            System.out.println("🌐 Cerrando " + openBrowsers.size() + " navegador(es)...");
            for (Browser browser : new ArrayList<>(openBrowsers)) {
                try {
                    browser.close();
                    System.out.println("   ✓ Navegador cerrado");
                } catch (Exception e) {
                    System.err.println("   ✗ Error cerrando navegador: " + e.getMessage());
                }
            }
            openBrowsers.clear();
        }

        // Cerrar todas las instancias de Playwright
        if (!playwrights.isEmpty()) {
            System.out.println("🎭 Cerrando " + playwrights.size() + " instancia(s) de Playwright...");
            for (Playwright pw : new ArrayList<>(playwrights)) {
                try {
                    pw.close();
                    System.out.println("   ✓ Playwright cerrado");
                } catch (Exception e) {
                    System.err.println("   ✗ Error cerrando Playwright: " + e.getMessage());
                }
            }
            playwrights.clear();
        }

        // Shutdown de todos los ExecutorServices
        if (!executorServices.isEmpty()) {
            System.out.println("⚙️  Deteniendo " + executorServices.size() + " ExecutorService(s)...");
            for (ExecutorService executor : new ArrayList<>(executorServices)) {
                try {
                    if (!executor.isShutdown()) {
                        executor.shutdown();
                        // Esperar a que terminen las tareas
                        if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                            System.out.println("   ⚠️  Forzando cierre de ExecutorService...");
                            executor.shutdownNow();
                        }
                        System.out.println("   ✓ ExecutorService detenido");
                    }
                } catch (Exception e) {
                    System.err.println("   ✗ Error deteniendo ExecutorService: " + e.getMessage());
                }
            }
            executorServices.clear();
        }

        System.out.println("\n✅ Limpieza de recursos completada\n");
    }

    /**
     * Obtiene el número de recursos abiertos (útil para debugging)
     */
    public String getResourceStats() {
        return String.format(
            "Navegadores: %d | Contextos: %d | Páginas: %d | ExecutorServices: %d | Playwrights: %d",
            openBrowsers.size(), openContexts.size(), openPages.size(),
            executorServices.size(), playwrights.size()
        );
    }
}
