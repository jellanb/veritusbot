package com.example.veritusbot.dto;

import java.util.UUID;

/**
 * DTO liviano para poblar el selector de usuarios en frontend.
 */
public class UsuarioLoginOptionDTO {

    private UUID id;
    private String email;
    private String nombreCompleto;
    private boolean activo;

    public UsuarioLoginOptionDTO() {
    }

    public UsuarioLoginOptionDTO(UUID id, String email, String nombreCompleto) {
        this.id = id;
        this.email = email;
        this.nombreCompleto = nombreCompleto;
    }

    public UsuarioLoginOptionDTO(UUID id, String email, String nombreCompleto, boolean activo) {
        this.id = id;
        this.email = email;
        this.nombreCompleto = nombreCompleto;
        this.activo = activo;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}

