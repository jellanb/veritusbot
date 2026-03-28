package com.example.veritusbot.dto;

import java.time.LocalDateTime;

/**
 * DTO para devolver respuesta exitosa de login.
 * Contiene el token JWT y la información básica del usuario.
 */
public class LoginResponseDTO {

    private String token;
    private UsuarioDTO usuario;
    private LocalDateTime loginAt;
    private String mensaje;

    // ==================== CONSTRUCTORES ====================

    public LoginResponseDTO() {
    }

    public LoginResponseDTO(String token, UsuarioDTO usuario) {
        this.token = token;
        this.usuario = usuario;
        this.loginAt = LocalDateTime.now();
        this.mensaje = "Login exitoso";
    }

    // ==================== GETTERS ====================

    public String getToken() {
        return token;
    }

    public UsuarioDTO getUsuario() {
        return usuario;
    }

    public LocalDateTime getLoginAt() {
        return loginAt;
    }

    public String getMensaje() {
        return mensaje;
    }

    // ==================== SETTERS ====================

    public void setToken(String token) {
        this.token = token;
    }

    public void setUsuario(UsuarioDTO usuario) {
        this.usuario = usuario;
    }

    public void setLoginAt(LocalDateTime loginAt) {
        this.loginAt = loginAt;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    @Override
    public String toString() {
        return "LoginResponseDTO{" +
                "usuario=" + usuario +
                ", loginAt=" + loginAt +
                '}';
    }
}

