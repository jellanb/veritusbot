package com.example.veritusbot.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity to track tribunal searches for persons
 * Used to verify that all tribunals in a search phase completed successfully
 * Before marking a person as processed
 */
@Entity
@Table(name = "tribunal_busquedas", uniqueConstraints = {
    @UniqueConstraint(
        name = "uk_tribunal_busqueda_request_person_year_phase_name",
        columnNames = {"persona_procesada_id", "solicitud_id", "anno", "fase", "tribunal_nombre"}
    )
}, indexes = {
    @Index(name = "idx_persona_request_fase", columnList = "persona_procesada_id,solicitud_id,fase"),
    @Index(name = "idx_persona_anno_fase", columnList = "persona_procesada_id,anno,fase"),
    @Index(name = "idx_solicitud_id", columnList = "solicitud_id"),
    @Index(name = "idx_estado", columnList = "estado")
})
public class TribunalBusqueda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "persona_procesada_id", nullable = false)
    private Integer personaProcessadaId;

    @Column(name = "solicitud_id", nullable = false, length = 100)
    private String solicitudId;

    @Column(name = "anno", nullable = false)
    private Integer anno;

    @Column(name = "fase", nullable = false, length = 20)
    private String fase; // "PHASE_1", "PHASE_2"

    @Column(name = "tribunal_nombre", nullable = false, length = 255)
    private String tribunalNombre;

    @Column(name = "tribunal_index")
    private Integer tribunalIndex;

    @Column(name = "estado", nullable = false, length = 30)
    private String estado; // "PENDIENTE", "COMPLETADA", "ERROR_SCRAPER", "ERROR_CONEXION"

    @Column(name = "creada", nullable = false)
    private LocalDateTime creada;

    @Column(name = "procesada_en")
    private LocalDateTime procesadaEn;

    @Column(name = "motivo_error", length = 500)
    private String motivoError;

    // Constructors
    public TribunalBusqueda() {
    }

    public TribunalBusqueda(Integer personaProcessadaId, String solicitudId, Integer anno, String fase,
                           String tribunalNombre, Integer tribunalIndex) {
        this.personaProcessadaId = personaProcessadaId;
        this.solicitudId = solicitudId;
        this.anno = anno;
        this.fase = fase;
        this.tribunalNombre = tribunalNombre;
        this.tribunalIndex = tribunalIndex;
        this.estado = "PENDIENTE";
        this.creada = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getPersonaProcessadaId() {
        return personaProcessadaId;
    }

    public void setPersonaProcessadaId(Integer personaProcessadaId) {
        this.personaProcessadaId = personaProcessadaId;
    }

    public String getSolicitudId() {
        return solicitudId;
    }

    public void setSolicitudId(String solicitudId) {
        this.solicitudId = solicitudId;
    }

    public Integer getAnno() {
        return anno;
    }

    public void setAnno(Integer anno) {
        this.anno = anno;
    }

    public String getFase() {
        return fase;
    }

    public void setFase(String fase) {
        this.fase = fase;
    }

    public String getTribunalNombre() {
        return tribunalNombre;
    }

    public void setTribunalNombre(String tribunalNombre) {
        this.tribunalNombre = tribunalNombre;
    }

    public Integer getTribunalIndex() {
        return tribunalIndex;
    }

    public void setTribunalIndex(Integer tribunalIndex) {
        this.tribunalIndex = tribunalIndex;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDateTime getCreada() {
        return creada;
    }

    public void setCreada(LocalDateTime creada) {
        this.creada = creada;
    }

    public LocalDateTime getProcesadaEn() {
        return procesadaEn;
    }

    public void setProcesadaEn(LocalDateTime procesadaEn) {
        this.procesadaEn = procesadaEn;
    }

    public String getMotivoError() {
        return motivoError;
    }

    public void setMotivoError(String motivoError) {
        this.motivoError = motivoError;
    }
}

