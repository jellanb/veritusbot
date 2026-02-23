package com.example.veritusbot.dto;

public class RolDTO {

    private String tipo;
    private String numero;
    private String anio;

    public RolDTO(String tipo, String numero, String anio) {
        this.tipo = tipo;
        this.numero = numero;
        this.anio = anio;
    }

    public String getTipo() {
        return tipo;
    }

    public String getNumero() {
        return numero;
    }

    public String getAnio() {
        return anio;
    }
}
