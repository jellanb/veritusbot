package com.example.veritusbot.dto;

public class ResultDTO {
    private String personName;
    private String tribunal;
    private int year;
    private String resolution;
    private String details;

    public ResultDTO(String personName, String tribunal, int year,
                     String resolution, String details) {
        this.personName = personName;
        this.tribunal = tribunal;
        this.year = year;
        this.resolution = resolution;
        this.details = details;
    }

    // Getters
    public String getPersonName() {
        return personName;
    }

    public String getTribunal() {
        return tribunal;
    }

    public int getYear() {
        return year;
    }

    public String getResolution() {
        return resolution;
    }

    public String getDetails() {
        return details;
    }

    @Override
    public String toString() {
        return "ResultDTO{" +
                "personName='" + personName + '\'' +
                ", tribunal='" + tribunal + '\'' +
                ", year=" + year +
                ", resolution='" + resolution + '\'' +
                ", details='" + details + '\'' +
                '}';
    }
}

