package com.example.veritusbot.dto;

import java.util.List;

/**
 * Respuesta paginada para el endpoint de reporte de causas.
 */
public class CausaReportResponseDTO {
    private List<CausaReportItemDTO> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private String sortBy;
    private String sortDir;

    public CausaReportResponseDTO(List<CausaReportItemDTO> content,
                                  int page,
                                  int size,
                                  long totalElements,
                                  int totalPages,
                                  String sortBy,
                                  String sortDir) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.sortBy = sortBy;
        this.sortDir = sortDir;
    }

    public List<CausaReportItemDTO> getContent() {
        return content;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public String getSortBy() {
        return sortBy;
    }

    public String getSortDir() {
        return sortDir;
    }
}

