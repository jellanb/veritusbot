package com.example.veritusbot.dto;

public class CausaDTO {
    private String rol;
    private String tribunal;
    private String caratula;
    private String estado;
    private String fecha;
    // Datos de búsqueda
    private String nombres;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private int ano;

    public CausaDTO() {
    }

    public CausaDTO(String rol, String tribunal, String caratula, String fecha,
                    String nombres, String apellidoPaterno, String apellidoMaterno, int ano) {
        this.rol = rol;
        this.tribunal = tribunal;
        this.caratula = caratula;
        this.fecha = fecha;
        this.nombres = nombres;
        this.apellidoPaterno = apellidoPaterno;
        this.apellidoMaterno = apellidoMaterno;
        this.ano = ano;
    }

    // Getters y Setters
    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public String getTribunal() {
        return tribunal;
    }

    public void setTribunal(String tribunal) {
        this.tribunal = tribunal;
    }

    public String getCaratula() {
        return caratula;
    }

    public void setCaratula(String caratula) {
        this.caratula = caratula;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

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

    public int getAno() {
        return ano;
    }

    public void setAno(int ano) {
        this.ano = ano;
    }

    @Override
    public String toString() {
        return "CausaDTO{" +
                "rol='" + rol + '\'' +
                ", tribunal='" + tribunal + '\'' +
                ", caratula='" + caratula + '\'' +
                ", fecha='" + fecha + '\'' +
                ", nombres='" + nombres + '\'' +
                ", apellidoPaterno='" + apellidoPaterno + '\'' +
                ", apellidoMaterno='" + apellidoMaterno + '\'' +
                ", ano=" + ano +
                '}';
    }
}
