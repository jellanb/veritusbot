package com.example.veritusbot.dto;

/**
 * DTO para actualizar en memoria si el control de horario activo está habilitado.
 */
public class UpdateWorkingHoursRequestDTO {

    private Boolean enabled;

    public UpdateWorkingHoursRequestDTO() {
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isValid() {
        return enabled != null;
    }
}

