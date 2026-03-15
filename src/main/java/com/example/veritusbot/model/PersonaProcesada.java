package com.example.veritusbot.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "personas_procesadas")
public class PersonaProcesada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "primer_nombre")
    private String primerNombre;

    @Column(name = "segundo_nombre")
    private String segundoNombre;

    @Column(name = "apellido_paterno")
    private String apellidoPaterno;

    @Column(name = "apellido_materno")
    private String apellidoMaterno;

    @Column(name = "rut")
    private String rut;

    @Column(name = "procesado")
    private Boolean procesado;

    @Column(name = "creada")
    private LocalDateTime creada;

    @Column(name = "fecha_procesada")
    private LocalDateTime fechaProcesada;

    @Column(name = "tribunal_principal_procesado")
    private Boolean tribunalPrincipalProcesado;

    @Column(name = "ultima_revalidacion")
    private LocalDateTime ultimaRevalidacion;

    @Column(name = "veces_revalidado")
    private Integer vecesRevalidado;

    // ...existing code...
    public PersonaProcesada() {
    }

    public PersonaProcesada(String primerNombre, String segundoNombre,
                           String apellidoPaterno, String apellidoMaterno) {
        this.primerNombre = primerNombre;
        this.segundoNombre = segundoNombre;
        this.apellidoPaterno = apellidoPaterno;
        this.apellidoMaterno = apellidoMaterno;
        this.procesado = false;
        this.creada = LocalDateTime.now();
    }

    // Getters y Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getPrimerNombre() {
        return primerNombre;
    }

    public void setPrimerNombre(String primerNombre) {
        this.primerNombre = primerNombre;
    }

    public String getSegundoNombre() {
        return segundoNombre;
    }

    public void setSegundoNombre(String segundoNombre) {
        this.segundoNombre = segundoNombre;
    }

    public String getApellidoPaterno() {
        return apellidoPaterno;
    }

    public void setApellidoPaterno(String apellidoPaterno) {
        this.apellidoPaterno = apellidoPaterno;
    }

    public String getApellidoMaterno() {
        return apellidoMaterno;
    }

    public void setApellidoMaterno(String apellidoMaterno) {
        this.apellidoMaterno = apellidoMaterno;
    }

    public String getRut() {
        return rut;
    }

    public void setRut(String rut) {
        this.rut = rut;
    }

    public Boolean getProcesado() {
        return procesado;
    }

    public void setProcesado(Boolean procesado) {
        this.procesado = procesado;
    }

    public LocalDateTime getCreada() {
        return creada;
    }

    public void setCreada(LocalDateTime creada) {
        this.creada = creada;
    }

    public LocalDateTime getFechaProcesada() {
        return fechaProcesada;
    }

    public void setFechaProcesada(LocalDateTime fechaProcesada) {
        this.fechaProcesada = fechaProcesada;
    }

    public Boolean getTribunalPrincipalProcesado() {
        return tribunalPrincipalProcesado;
    }

    public void setTribunalPrincipalProcesado(Boolean tribunalPrincipalProcesado) {
        this.tribunalPrincipalProcesado = tribunalPrincipalProcesado;
    }

    public LocalDateTime getUltimaRevalidacion() {
        return ultimaRevalidacion;
    }

    public void setUltimaRevalidacion(LocalDateTime ultimaRevalidacion) {
        this.ultimaRevalidacion = ultimaRevalidacion;
    }

    public Integer getVecesRevalidado() {
        return vecesRevalidado;
    }

    public void setVecesRevalidado(Integer vecesRevalidado) {
        this.vecesRevalidado = vecesRevalidado;
    }

    @Override
    public String toString() {
        return "PersonaProcesada{" +
                "id=" + id +
                ", primerNombre='" + primerNombre + '\'' +
                ", segundoNombre='" + segundoNombre + '\'' +
                ", apellidoPaterno='" + apellidoPaterno + '\'' +
                ", apellidoMaterno='" + apellidoMaterno + '\'' +
                ", rut='" + rut + '\'' +
                ", procesado=" + procesado +
                ", creada=" + creada +
                ", fechaProcesada=" + fechaProcesada +
                ", tribunalPrincipalProcesado=" + tribunalPrincipalProcesado +
                '}';
    }
}

