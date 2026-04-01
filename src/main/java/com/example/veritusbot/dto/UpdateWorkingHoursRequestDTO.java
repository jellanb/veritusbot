package com.example.veritusbot.dto;

/**
 * DTO para actualizar en memoria si el control de horario activo está habilitado.
 */
public class UpdateWorkingHoursRequestDTO {

    private Boolean enabled;
    private String horaInicio;
    private String horaFin;

    public UpdateWorkingHoursRequestDTO() {
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(String horaInicio) {
        this.horaInicio = horaInicio;
    }

    public String getHoraFin() {
        return horaFin;
    }

    public void setHoraFin(String horaFin) {
        this.horaFin = horaFin;
    }

    public boolean isValid() {
        return enabled != null;
    }
}

