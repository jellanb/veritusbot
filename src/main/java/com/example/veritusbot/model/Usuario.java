package com.example.veritusbot.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad que representa un usuario del sistema Veritus Bot.
 * Gestiona autenticación, roles y estado del usuario.
 *
 * SOLID Principle - Single Responsibility:
 * Solo es responsable de almacenar datos de usuario, no lógica de autenticación.
 */
@Entity
@Table(name = "usuarios", indexes = {
        @Index(name = "idx_usuarios_email", columnList = "email", unique = true)
})
public class Usuario {

    @Id
    @Column(name = "id", columnDefinition = "UNIQUEIDENTIFIER")
    private UUID id;

    @Column(name = "email", length = 255, nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", length = 255, nullable = false)
    private String passwordHash;

    @Column(name = "nombre_completo", length = 255)
    private String nombreCompleto;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false, length = 50)
    private RolUsuario rol;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 50)
    private EstadoUsuario estado;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    /**
     * Constructor por defecto.
     * Inicializa ID único y timestamp de creación.
     */
    public Usuario() {
        this.id = UUID.randomUUID();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Constructor con parámetros principales.
     */
    public Usuario(String email, String passwordHash, String nombreCompleto, RolUsuario rol) {
        this();
        this.email = email;
        this.passwordHash = passwordHash;
        this.nombreCompleto = nombreCompleto;
        this.rol = rol;
        this.estado = EstadoUsuario.ACTIVO;
    }

    // ==================== GETTERS ====================

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public RolUsuario getRol() {
        return rol;
    }

    public EstadoUsuario getEstado() {
        return estado;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    // ==================== SETTERS ====================

    public void setEmail(String email) {
        this.email = email;
        this.updatedAt = LocalDateTime.now();
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
        this.updatedAt = LocalDateTime.now();
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
        this.updatedAt = LocalDateTime.now();
    }

    public void setRol(RolUsuario rol) {
        this.rol = rol;
        this.updatedAt = LocalDateTime.now();
    }

    public void setEstado(EstadoUsuario estado) {
        this.estado = estado;
        this.updatedAt = LocalDateTime.now();
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
        this.updatedAt = LocalDateTime.now();
    }

    // ==================== MÉTODOS DE NEGOCIO ====================

    /**
     * Verifica si el usuario está activo y puede acceder al sistema.
     *
     * @return true si el usuario está ACTIVO
     */
    public boolean isActivo() {
        return this.estado == EstadoUsuario.ACTIVO;
    }

    /**
     * Verifica si el usuario está bloqueado.
     *
     * @return true si el usuario está BLOQUEADO
     */
    public boolean isBloqueado() {
        return this.estado == EstadoUsuario.BLOQUEADO;
    }

    /**
     * Verifica si el usuario es administrador.
     *
     * @return true si el rol es ADMIN
     */
    public boolean isAdmin() {
        return this.rol == RolUsuario.ADMIN;
    }

    /**
     * Verifica si el usuario puede ejecutar operaciones (ADMIN o OPERADOR).
     *
     * @return true si puede ejecutar operaciones
     */
    public boolean canOperate() {
        return this.rol == RolUsuario.ADMIN || this.rol == RolUsuario.OPERADOR;
    }

    @Override
    public String toString() {
        return "Usuario{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", nombreCompleto='" + nombreCompleto + '\'' +
                ", rol=" + rol +
                ", estado=" + estado +
                ", lastLogin=" + lastLogin +
                '}';
    }
}

