package com.example.veritusbot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO de respuesta para el estado del dashboard.
 */
public class DashboardStatusResponseDTO {

    private final BackendStatusDTO backend;
    private final SearchTypesStatusDTO searchTypes;
    private final WorkloadStatusDTO workload;

    public DashboardStatusResponseDTO(BackendStatusDTO backend,
                                      SearchTypesStatusDTO searchTypes,
                                      WorkloadStatusDTO workload) {
        this.backend = backend;
        this.searchTypes = searchTypes;
        this.workload = workload;
    }

    public BackendStatusDTO getBackend() {
        return backend;
    }

    public SearchTypesStatusDTO getSearchTypes() {
        return searchTypes;
    }

    public WorkloadStatusDTO getWorkload() {
        return workload;
    }

    public static class BackendStatusDTO {
        @JsonProperty("isWorking")
        private final boolean isWorking;
        private final String activeSearchId;
        private final String activeSearchName;
        private final String startedAt;
        private final String lastUpdatedAt;

        public BackendStatusDTO(boolean isWorking,
                                String activeSearchId,
                                String activeSearchName,
                                String startedAt,
                                String lastUpdatedAt) {
            this.isWorking = isWorking;
            this.activeSearchId = activeSearchId;
            this.activeSearchName = activeSearchName;
            this.startedAt = startedAt;
            this.lastUpdatedAt = lastUpdatedAt;
        }

        public boolean isWorking() {
            return isWorking;
        }

        public String getActiveSearchId() {
            return activeSearchId;
        }

        public String getActiveSearchName() {
            return activeSearchName;
        }

        public String getStartedAt() {
            return startedAt;
        }

        public String getLastUpdatedAt() {
            return lastUpdatedAt;
        }
    }

    public static class SearchTypesStatusDTO {
        private final int santiagoCount;
        private final int regionesCount;

        public SearchTypesStatusDTO(int santiagoCount, int regionesCount) {
            this.santiagoCount = santiagoCount;
            this.regionesCount = regionesCount;
        }

        public int getSantiagoCount() {
            return santiagoCount;
        }

        public int getRegionesCount() {
            return regionesCount;
        }
    }

    public static class WorkloadStatusDTO {
        private final int activeSearches;
        private final int foundResults;

        public WorkloadStatusDTO(int activeSearches, int foundResults) {
            this.activeSearches = activeSearches;
            this.foundResults = foundResults;
        }

        public int getActiveSearches() {
            return activeSearches;
        }

        public int getFoundResults() {
            return foundResults;
        }
    }
}
