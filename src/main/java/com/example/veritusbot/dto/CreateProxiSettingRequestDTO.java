package com.example.veritusbot.dto;

/**
 * DTO para recibir los datos de un nuevo proxy desde el frontend.
 */
public class CreateProxiSettingRequestDTO {

    private String server;
    private String username;
    private String password;
    private Boolean activo;
    private Integer orden;

    // ==================== CONSTRUCTORES ====================

    public CreateProxiSettingRequestDTO() {
    }

    // ==================== GETTERS ====================

    public String getServer() {
        return server;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public Boolean getActivo() {
        return activo;
    }

    public Integer getOrden() {
        return orden;
    }

    // ==================== SETTERS ====================

    public void setServer(String server) {
        this.server = server;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public void setOrden(Integer orden) {
        this.orden = orden;
    }

    // ==================== VALIDACIÓN ====================

    /**
     * Valida que el campo obligatorio server no esté vacío y tenga formato mínimo válido.
     *
     * @return true si el request es válido
     */
    public boolean isValid() {
        return server != null && !server.trim().isEmpty();
    }

    @Override
    public String toString() {
        return "CreateProxiSettingRequestDTO{server='" + server + "', username='" + username + "', orden=" + orden + "}";
    }
}

