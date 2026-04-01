package com.example.veritusbot.service.scraper.browser;

import com.example.veritusbot.model.ProxiSetting;
import com.example.veritusbot.service.ProxiSettingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ProxySelectorService {
    private static final Logger logger = LoggerFactory.getLogger(ProxySelectorService.class);

    private final AtomicInteger nextProxyIndex = new AtomicInteger(0);
    private final ProxiSettingService proxiSettingService;

    public record ProxyConfig(String server, String username, String password) {
    }

    public ProxySelectorService(ProxiSettingService proxiSettingService) {
        this.proxiSettingService = proxiSettingService;
    }

    public ProxyConfig pickNextProxyOrNull() {
        List<ProxiSetting> activos = proxiSettingService.listarActivos();
        logger.debug("🌍 Configured proxies available: {}", activos.size());

        if (activos.isEmpty()) {
            logger.debug("🌍 No proxy selected because configuration is empty");
            return null;
        }

        int selectedIndex = Math.floorMod(nextProxyIndex.getAndIncrement(), activos.size());
        ProxiSetting selected = activos.get(selectedIndex);
        logger.debug("🌍 Selected proxy index {} -> {}", selectedIndex, selected.getServer());
        return toProxyConfig(selected);
    }

    private ProxyConfig toProxyConfig(ProxiSetting setting) {
        String normalizedServer = setting.getServer().trim();
        String normalizedUser = normalizeCredential(setting.getUsername());
        String normalizedPass = normalizeCredential(setting.getPassword());
        logger.debug("🌍 Registered proxy {} (auth={})", normalizedServer, normalizedUser != null);
        return new ProxyConfig(normalizedServer, normalizedUser, normalizedPass);
    }

    private String normalizeCredential(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
