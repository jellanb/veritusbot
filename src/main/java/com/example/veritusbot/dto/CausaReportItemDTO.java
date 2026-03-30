package com.example.veritusbot.dto;

import java.util.UUID;

/**
 * Item de salida para el reporte de causas.
 */
public class CausaReportItemDTO {
    private UUID id;
    private UUID personaId;
    private String rol;
    private Integer anio;
    private String caratula;
    private String tribunal;

    public CausaReportItemDTO(UUID id, UUID personaId, String rol, Integer anio, String caratula, String tribunal) {
        this.id = id;
        this.personaId = personaId;
        this.rol = rol;
        this.anio = anio;
        this.caratula = caratula;
        this.tribunal = tribunal;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPersonaId() {
        return personaId;
    }

    public String getRol() {
        return rol;
    }

    public Integer getAnio() {
        return anio;
    }

    public String getCaratula() {
        return caratula;
    }

    public String getTribunal() {
        return tribunal;
    }
}

