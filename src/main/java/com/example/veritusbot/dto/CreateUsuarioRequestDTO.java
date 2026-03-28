package com.example.veritusbot.dto;

/**
 * DTO para recibir los datos de un nuevo usuario desde el frontend.
 */
public class CreateUsuarioRequestDTO {

    private String email;
    private String password;
    private String nombreCompleto;
    private String rol;  // "ADMIN", "OPERADOR", "VIEWER"

    // ==================== CONSTRUCTORES ====================

    public CreateUsuarioRequestDTO() {
    }

    // ==================== GETTERS ====================

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public String getRol() {
        return rol;
    }

    // ==================== SETTERS ====================

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    // ==================== VALIDACIÓN ====================

    /**
     * Valida que los campos obligatorios no estén vacíos.
     *
     * @return true si el request es válido
     */
    public boolean isValid() {
        return email != null && !email.trim().isEmpty()
                && password != null && password.trim().length() >= 6
                && nombreCompleto != null && !nombreCompleto.trim().isEmpty();
    }

    @Override
    public String toString() {
        return "CreateUsuarioRequestDTO{email='" + email + "', nombreCompleto='" + nombreCompleto + "', rol='" + rol + "'}";
    }
}

