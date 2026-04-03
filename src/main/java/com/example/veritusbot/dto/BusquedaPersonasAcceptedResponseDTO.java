package com.example.veritusbot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BusquedaPersonasAcceptedResponseDTO {

    private String status;
    private String message;
    private String requestId;
    private int peopleCount;
    @JsonProperty("isAllRegionEnabled")
    private boolean isAllRegionEnabled;

    @JsonProperty("isSantiagoEnabled")
    private boolean isSantiagoEnabled;
    private int threadsPerPerson;
    private String processingMessage;
    private String searchName;

    public BusquedaPersonasAcceptedResponseDTO() {
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public int getPeopleCount() {
        return peopleCount;
    }

    public void setPeopleCount(int peopleCount) {
        this.peopleCount = peopleCount;
    }

    public boolean isAllRegionEnabled() {
        return isAllRegionEnabled;
    }

    public void setAllRegionEnabled(boolean allRegionEnabled) {
        isAllRegionEnabled = allRegionEnabled;
    }

    public boolean isSantiagoEnabled() {
        return isSantiagoEnabled;
    }

    public void setSantiagoEnabled(boolean santiagoEnabled) {
        isSantiagoEnabled = santiagoEnabled;
    }

    public int getThreadsPerPerson() {
        return threadsPerPerson;
    }

    public void setThreadsPerPerson(int threadsPerPerson) {
        this.threadsPerPerson = threadsPerPerson;
    }

    public String getProcessingMessage() {
        return processingMessage;
    }

    public void setProcessingMessage(String processingMessage) {
        this.processingMessage = processingMessage;
    }

    public String getSearchName() {
        return searchName;
    }

    public void setSearchName(String searchName) {
        this.searchName = searchName;
    }
}

