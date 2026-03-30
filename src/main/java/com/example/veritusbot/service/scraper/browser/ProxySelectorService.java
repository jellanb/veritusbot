package com.example.veritusbot.service.scraper.browser;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class ProxySelectorService {

    public record ProxyConfig(String server, String username, String password) {
    }

    @Value("${proxi1:}")
    private String proxi1;
    @Value("${userProxi1:}")
    private String userProxi1;
    @Value("${passProxi1:}")
    private String passProxi1;

    @Value("${proxi2:}")
    private String proxi2;
    @Value("${userProxi2:}")
    private String userProxi2;
    @Value("${passProxi2:}")
    private String passProxi2;

    @Value("${proxi3:}")
    private String proxi3;
    @Value("${userProxi3:}")
    private String userProxi3;
    @Value("${passProxi3:}")
    private String passProxi3;

    @Value("${proxi4:}")
    private String proxi4;
    @Value("${userProxi4:}")
    private String userProxi4;
    @Value("${passProxi4:}")
    private String passProxi4;

    @Value("${proxi5:}")
    private String proxi5;
    @Value("${userProxi5:}")
    private String userProxi5;
    @Value("${passProxi5:}")
    private String passProxi5;

    @Value("${proxi6:}")
    private String proxi6;
    @Value("${userProxi6:}")
    private String userProxi6;
    @Value("${passProxi6:}")
    private String passProxi6;

    public ProxyConfig pickRandomProxyOrNull() {
        List<ProxyConfig> configuredProxies = new ArrayList<>(6);
        addIfPresent(configuredProxies, proxi1, userProxi1, passProxi1);
        addIfPresent(configuredProxies, proxi2, userProxi2, passProxi2);
        addIfPresent(configuredProxies, proxi3, userProxi3, passProxi3);
        addIfPresent(configuredProxies, proxi4, userProxi4, passProxi4);
        addIfPresent(configuredProxies, proxi5, userProxi5, passProxi5);
        addIfPresent(configuredProxies, proxi6, userProxi6, passProxi6);

        if (configuredProxies.isEmpty()) {
            return null;
        }

        int selectedIndex = ThreadLocalRandom.current().nextInt(configuredProxies.size());
        return configuredProxies.get(selectedIndex);
    }

    private void addIfPresent(List<ProxyConfig> proxies, String server, String username, String password) {
        if (server == null || server.isBlank()) {
            return;
        }

        String normalizedServer = server.trim();
        String normalizedUser = normalizeCredentialsValue(username);
        String normalizedPass = normalizeCredentialsValue(password);

        proxies.add(new ProxyConfig(normalizedServer, normalizedUser, normalizedPass));
    }

    private String normalizeCredentialsValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}

