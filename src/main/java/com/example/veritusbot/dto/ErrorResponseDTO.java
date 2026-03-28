package com.example.veritusbot.dto;

import java.time.LocalDateTime;

/**
 * DTO para devolver respuestas de error de la API.
 * Estandariza el formato de errores en toda la aplicación.
 */
public class ErrorResponseDTO {

    private String mensaje;
    private String codigo;
    private LocalDateTime timestamp;
    private String detalle;

    // ==================== CONSTRUCTORES ====================

    public ErrorResponseDTO() {
        this.timestamp = LocalDateTime.now();
    }

    public ErrorResponseDTO(String mensaje) {
        this.mensaje = mensaje;
        this.codigo = "ERROR_GENERICO";
        this.timestamp = LocalDateTime.now();
    }

    public ErrorResponseDTO(String mensaje, String codigo) {
        this.mensaje = mensaje;
        this.codigo = codigo;
        this.timestamp = LocalDateTime.now();
    }

    public ErrorResponseDTO(String mensaje, String codigo, String detalle) {
        this.mensaje = mensaje;
        this.codigo = codigo;
        this.detalle = detalle;
        this.timestamp = LocalDateTime.now();
    }

    // ==================== GETTERS ====================

    public String getMensaje() {
        return mensaje;
    }

    public String getCodigo() {
        return codigo;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getDetalle() {
        return detalle;
    }

    // ==================== SETTERS ====================

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }

    @Override
    public String toString() {
        return "ErrorResponseDTO{" +
                "mensaje='" + mensaje + '\'' +
                ", codigo='" + codigo + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}

