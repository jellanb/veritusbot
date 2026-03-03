package com.example.veritusbot.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "causas")
public class Causa {

    @Id
    @Column(name = "id", columnDefinition = "UNIQUEIDENTIFIER")
    private UUID id = UUID.randomUUID();

    @Column(name = "persona_id", columnDefinition = "UNIQUEIDENTIFIER")
    private UUID personaId;

    @Column(name = "rol", length = 50)
    private String rol;

    @Column(name = "anio")
    private Integer anio;

    @Column(name = "caratula", length = 255)
    private String caratula;

    @Column(name = "tribunal", length = 255)
    private String tribunal;

    // Constructores
    public Causa() {
        this.id = UUID.randomUUID();
    }

    public Causa(UUID personaId, String rol, Integer anio, String caratula, String tribunal) {
        this.id = UUID.randomUUID();
        this.personaId = personaId;
        this.rol = rol;
        this.anio = anio;
        this.caratula = caratula;
        this.tribunal = tribunal;
    }

    // Getters y Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getPersonaId() {
        return personaId;
    }

    public void setPersonaId(UUID personaId) {
        this.personaId = personaId;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public Integer getAnio() {
        return anio;
    }

    public void setAnio(Integer anio) {
        this.anio = anio;
    }

    public String getCaratula() {
        return caratula;
    }

    public void setCaratula(String caratula) {
        this.caratula = caratula;
    }

    public String getTribunal() {
        return tribunal;
    }

    public void setTribunal(String tribunal) {
        this.tribunal = tribunal;
    }
}
