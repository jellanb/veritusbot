package com.example.veritusbot.model;

/**
 * Enum de roles disponibles para usuarios en el sistema.
 * Define los niveles de acceso y permisos en la aplicación.
 */
public enum RolUsuario {
    /**
     * Acceso total al sistema.
     * Puede: crear usuarios, iniciar búsquedas, ver reportes, etc.
     */
    ADMIN("Administrador"),

    /**
     * Puede ejecutar búsquedas y ver resultados.
     * Permisos limitados a operaciones básicas.
     */
    OPERADOR("Operador"),

    /**
     * Solo lectura en el sistema.
     * Puede ver resultados pero no ejecutar búsquedas.
     */
    VIEWER("Visualizador");

    private final String descripcion;

    RolUsuario(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}

