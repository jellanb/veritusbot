package com.example.veritusbot.dto;

public class ProxiSettingDTO {

    private Long id;
    private String server;
    private String username;
    private String password;
    private boolean activo;
    private int orden;

    public ProxiSettingDTO() {
    }

    public ProxiSettingDTO(Long id, String server, String username, String password, boolean activo, int orden) {
        this.id = id;
        this.server = server;
        this.username = username;
        this.password = password;
        this.activo = activo;
        this.orden = orden;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public int getOrden() {
        return orden;
    }

    public void setOrden(int orden) {
        this.orden = orden;
    }
}

