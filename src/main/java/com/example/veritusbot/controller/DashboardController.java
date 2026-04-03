package com.example.veritusbot.controller;

import com.example.veritusbot.dto.DashboardStatusResponseDTO;
import com.example.veritusbot.service.DashboardStatusService;
import com.example.veritusbot.service.ProcessingStateManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Expone estado operativo del backend para consumo del dashboard frontend.
 */
@RestController
@RequestMapping("/api/veritus-app/dashboard")
public class DashboardController {

    private final DashboardStatusService dashboardStatusService;
    private final ProcessingStateManager processingStateManager;

    public DashboardController(DashboardStatusService dashboardStatusService,
                               ProcessingStateManager processingStateManager) {
        this.dashboardStatusService = dashboardStatusService;
        this.processingStateManager = processingStateManager;
    }

    @GetMapping("/status")
    public ResponseEntity<DashboardStatusResponseDTO> getDashboardStatus() {
        boolean isWorking = processingStateManager.isProcessing();
        return ResponseEntity.ok(dashboardStatusService.getStatus(isWorking));
    }
}

