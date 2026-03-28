package com.example.veritusbot.model;

/**
 * Enum que define los posibles estados de un usuario en el sistema.
 */
public enum EstadoUsuario {
    /**
     * Usuario activo y puede acceder al sistema.
     */
    ACTIVO("Activo"),

    /**
     * Usuario inactivo pero no bloqueado.
     * Puede ser reactivado.
     */
    INACTIVO("Inactivo"),

    /**
     * Usuario bloqueado por razones de seguridad.
     * Requiere intervención del administrador.
     */
    BLOQUEADO("Bloqueado");

    private final String descripcion;

    EstadoUsuario(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}

