package com.example.veritusbot.dto;

public class PersonaDTO {
    private String nombres;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private int anioInit;
    private int anioFin;

    public PersonaDTO() {
    }

    public PersonaDTO(String nombres, String apellidoPaterno, String apellidoMaterno, int anioInit, int anioFin) {
        this.nombres = nombres;
        this.apellidoPaterno = apellidoPaterno;
        this.apellidoMaterno = apellidoMaterno;
        this.anioInit = anioInit;
        this.anioFin = anioFin;
    }

    // Getters y Setters
    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
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

    public int getAnioInit() {
        return anioInit;
    }

    public void setAnioInit(int anioInit) {
        this.anioInit = anioInit;
    }

    public int getAnioFin() {
        return anioFin;
    }

    public void setAnioFin(int anioFin) {
        this.anioFin = anioFin;
    }

    @Override
    public String toString() {
        return "PersonaDTO{" +
                "nombres='" + nombres + '\'' +
                ", apellidoPaterno='" + apellidoPaterno + '\'' +
                ", apellidoMaterno='" + apellidoMaterno + '\'' +
                ", anioInit=" + anioInit +
                ", anioFin=" + anioFin +
                '}';
    }
}
