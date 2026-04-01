package com.example.veritusbot.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class SearchRuntimeConfigService {

    private final AtomicInteger threadsPerPerson = new AtomicInteger(1);
    private final ProxiSettingService proxiSettingService;

    public SearchRuntimeConfigService(ProxiSettingService proxiSettingService) {
        this.proxiSettingService = proxiSettingService;
    }

    public int getThreadsPerPerson() {
        return threadsPerPerson.get();
    }

    public int updateThreadsPerPerson(int requestedThreadsPerPerson) {
        if (requestedThreadsPerPerson < 1) {
            throw new IllegalArgumentException("threadsPerPerson debe ser mayor o igual a 1");
        }

        int activeProxyCount = proxiSettingService.listarActivos().size();
        if (activeProxyCount <= 0) {
            throw new IllegalArgumentException("No hay proxies activos en DB. Configura al menos 1 proxy activo.");
        }

        if (requestedThreadsPerPerson > activeProxyCount) {
            throw new IllegalArgumentException("threadsPerPerson no puede ser mayor a la cantidad de proxies activos (" + activeProxyCount + ")");
        }

        threadsPerPerson.set(requestedThreadsPerPerson);
        return requestedThreadsPerPerson;
    }

    public Map<String, Object> getCurrentConfig() {
        int activeProxyCount = proxiSettingService.listarActivos().size();
        Map<String, Object> config = new HashMap<>();
        config.put("threadsPerPerson", threadsPerPerson.get());
        config.put("activeProxyCount", activeProxyCount);
        config.put("maxAllowedThreads", activeProxyCount);
        return config;
    }
}


