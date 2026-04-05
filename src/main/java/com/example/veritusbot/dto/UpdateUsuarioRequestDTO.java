package com.example.veritusbot.dto;

/**
 * DTO para actualizar datos administrables de un usuario existente.
 */
public class UpdateUsuarioRequestDTO {

    private String nombreCompleto;
    private String rol;
    private String estado;

    public UpdateUsuarioRequestDTO() {
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public boolean hasAnyFieldToUpdate() {
        return (nombreCompleto != null)
                || (rol != null)
                || (estado != null);
    }
}

