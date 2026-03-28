package com.example.veritusbot.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO para devolver información del usuario autenticado.
 * Nunca expone información sensible como contraseñas.
 */
public class UsuarioDTO {

    private UUID id;
    private String email;
    private String nombreCompleto;
    private String rol;
    private String estado;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;

    // ==================== CONSTRUCTORES ====================

    public UsuarioDTO() {
    }

    public UsuarioDTO(UUID id, String email, String nombreCompleto, String rol, String estado) {
        this.id = id;
        this.email = email;
        this.nombreCompleto = nombreCompleto;
        this.rol = rol;
        this.estado = estado;
    }

    // ==================== GETTERS ====================

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public String getRol() {
        return rol;
    }

    public String getEstado() {
        return estado;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    // ==================== SETTERS ====================

    public void setId(UUID id) {
        this.id = id;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    @Override
    public String toString() {
        return "UsuarioDTO{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", rol='" + rol + '\'' +
                ", estado='" + estado + '\'' +
                '}';
    }
}

