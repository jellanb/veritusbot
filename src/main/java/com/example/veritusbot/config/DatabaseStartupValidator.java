package com.example.veritusbot.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Validador de conexión a la base de datos al iniciar la aplicación
 * Si no puede conectarse, la app se apaga gracefully
 */
@Component
public class DatabaseStartupValidator {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private GracefulShutdownConfig gracefulShutdownConfig;

    @EventListener(ApplicationReadyEvent.class)
    public void validateDatabaseConnection() {
        System.out.println("\n╔════════════════════════════════════════════════════════════╗");
        System.out.println("║  VALIDANDO CONEXIÓN A BASE DE DATOS                         ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");

        try {
            Connection connection = dataSource.getConnection();
            if (connection != null) {
                System.out.println("✓ Conexión a Base de Datos establecida correctamente");
                System.out.println("  Database: " + connection.getMetaData().getDatabaseProductName());
                System.out.println("  Version: " + connection.getMetaData().getDatabaseProductVersion());
                connection.close();
                System.out.println("\n✓ Base de datos lista para usar\n");
            } else {
                throw new Exception("DataSource retornó null");
            }
        } catch (Exception e) {
            System.err.println("\n" + "═".repeat(60));
            System.err.println("❌ ERROR CRÍTICO: NO SE PUDO CONECTAR A LA BASE DE DATOS");
            System.err.println("═".repeat(60));
            System.err.println("Motivo: " + e.getMessage());
            System.err.println("\nVerifica:");
            System.err.println("  • ¿SQL Server está corriendo?");
            System.err.println("  • ¿Las credenciales en application.properties son correctas?");
            System.err.println("  • ¿El puerto está correctamente configurado?");
            System.err.println("\nEjemplo de comando para iniciar SQL Server con Podman:");
            System.err.println("  podman run -d \\");
            System.err.println("    --name sqlserver \\");
            System.err.println("    -e \"ACCEPT_EULA=Y\" \\");
            System.err.println("    -e \"MSSQL_SA_PASSWORD=SqlServer2026Strong\" \\");
            System.err.println("    -p 14333:1433 \\");
            System.err.println("    mcr.microsoft.com/mssql/server:2022-latest");
            System.err.println("═".repeat(60) + "\n");

            // Apagar la aplicación gracefully
            System.err.println("Apagando la aplicación...");
            gracefulShutdownConfig.initiateShutdown();
        }
    }
}

