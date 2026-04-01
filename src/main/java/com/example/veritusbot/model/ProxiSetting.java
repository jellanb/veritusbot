package com.example.veritusbot.model;

import jakarta.persistence.*;

@Entity
@Table(name = "proxi_setting", indexes = {
    @Index(name = "idx_proxi_setting_activo", columnList = "activo"),
    @Index(name = "idx_proxi_setting_orden", columnList = "orden")
})
public class ProxiSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "server", nullable = false, length = 255)
    private String server;

    @Column(name = "username", length = 100)
    private String username;

    @Column(name = "password", length = 100)
    private String password;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    @Column(name = "orden", nullable = false)
    private int orden = 0;

    public ProxiSetting() {
    }

    public ProxiSetting(String server, String username, String password, boolean activo, int orden) {
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

