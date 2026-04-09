package com.example.veritusbot.service.scraper.browser;

import com.example.veritusbot.model.ProxiSetting;
import com.example.veritusbot.service.ProxiSettingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class ProxySelectorService {
    private static final Logger logger = LoggerFactory.getLogger(ProxySelectorService.class);
    private static final Duration DEFAULT_WAIT_TIMEOUT = Duration.ofSeconds(30);

    private final AtomicInteger nextProxyIndex = new AtomicInteger(0);
    private final ProxiSettingService proxiSettingService;

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition proxyReleased = lock.newCondition();
    private final Set<String> leasedProxyServers = new HashSet<>();

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

    public ProxyConfig acquireExclusiveProxyOrNull() {
        return acquireExclusiveProxyOrNull(DEFAULT_WAIT_TIMEOUT);
    }

    public ProxyConfig acquireExclusiveProxyOrNull(Duration waitTimeout) {
        List<ProxiSetting> activos = proxiSettingService.listarActivos();
        if (activos.isEmpty()) {
            logger.debug("🌍 No proxy acquired because configuration is empty");
            return null;
        }

        long remainingNanos = waitTimeout.toNanos();
        lock.lock();
        try {
            while (true) {
                ProxyConfig selected = tryAcquireFrom(activos);
                if (selected != null) {
                    logger.debug("🔒 Acquired exclusive proxy: {}", selected.server());
                    return selected;
                }

                if (remainingNanos <= 0L) {
                    throw new IllegalStateException("No hay proxies disponibles en este momento. Intenta nuevamente.");
                }

                logger.debug("⏳ Waiting for a proxy to be released ({} active, {} leased)", activos.size(), leasedProxyServers.size());
                try {
                    remainingNanos = proxyReleased.awaitNanos(remainingNanos);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("La espera por un proxy fue interrumpida.", e);
                }

                activos = proxiSettingService.listarActivos();
                if (activos.isEmpty()) {
                    logger.debug("🌍 Proxy list became empty while waiting");
                    return null;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public void releaseExclusiveProxy(String server) {
        if (server == null || server.isBlank()) {
            return;
        }

        String normalizedServer = server.trim();
        lock.lock();
        try {
            if (leasedProxyServers.remove(normalizedServer)) {
                logger.debug("🔓 Released exclusive proxy: {}", normalizedServer);
                proxyReleased.signal();
            }
        } finally {
            lock.unlock();
        }
    }

    private ProxyConfig tryAcquireFrom(List<ProxiSetting> activos) {
        int size = activos.size();
        for (int i = 0; i < size; i++) {
            int selectedIndex = Math.floorMod(nextProxyIndex.getAndIncrement(), size);
            ProxyConfig candidate = toProxyConfig(activos.get(selectedIndex));
            if (leasedProxyServers.add(candidate.server())) {
                return candidate;
            }
        }
        return null;
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
