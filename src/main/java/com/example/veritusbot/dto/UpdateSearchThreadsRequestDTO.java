package com.example.veritusbot.dto;

/**
 * DTO para actualizar la configuración de hilos por persona en memoria.
 */
public class UpdateSearchThreadsRequestDTO {

    private Integer threadsPerPerson;

    public UpdateSearchThreadsRequestDTO() {
    }

    public Integer getThreadsPerPerson() {
        return threadsPerPerson;
    }

    public void setThreadsPerPerson(Integer threadsPerPerson) {
        this.threadsPerPerson = threadsPerPerson;
    }

    public boolean isValid() {
        return threadsPerPerson != null && threadsPerPerson > 0;
    }
}

