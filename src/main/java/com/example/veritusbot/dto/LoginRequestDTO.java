package com.example.veritusbot.dto;

/**
 * DTO para recibir credenciales de login del frontend.
 * Representa la solicitud de autenticación del usuario.
 */
public class LoginRequestDTO {

    private String email;
    private String password;

    // ==================== CONSTRUCTORES ====================

    public LoginRequestDTO() {
    }

    public LoginRequestDTO(String email, String password) {
        this.email = email;
        this.password = password;
    }

    // ==================== GETTERS ====================

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    // ==================== SETTERS ====================

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    // ==================== MÉTODOS ====================

    /**
     * Valida que los campos requeridos no estén vacíos.
     *
     * @return true si el request es válido
     */
    public boolean isValid() {
        return email != null && !email.trim().isEmpty() &&
               password != null && !password.trim().isEmpty();
    }

    @Override
    public String toString() {
        return "LoginRequestDTO{" +
                "email='" + email + '\'' +
                '}';
    }
}

