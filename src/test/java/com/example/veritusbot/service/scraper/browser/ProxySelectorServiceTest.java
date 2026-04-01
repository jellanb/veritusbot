package com.example.veritusbot.service.scraper.browser;

import com.example.veritusbot.model.ProxiSetting;
import com.example.veritusbot.service.ProxiSettingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProxySelectorServiceTest {

    @Mock
    private ProxiSettingService proxiSettingService;

    @InjectMocks
    private ProxySelectorService service;

    @Test
    void pickNextProxyOrNullShouldReturnNullWhenNoProxyConfigured() {
        when(proxiSettingService.listarActivos()).thenReturn(List.of());

        assertNull(service.pickNextProxyOrNull());
    }

    @Test
    void pickNextProxyOrNullShouldReturnConfiguredProxiesInSequenceAndWrapAround() {
        ProxiSetting proxy1 = new ProxiSetting("http://10.0.0.1:8080", "user1", "pass1", true, 1);
        ProxiSetting proxy2 = new ProxiSetting("http://10.0.0.2:8080", "user2", "pass2", true, 2);

        when(proxiSettingService.listarActivos()).thenReturn(List.of(proxy1, proxy2));

        ProxySelectorService.ProxyConfig first = service.pickNextProxyOrNull();
        ProxySelectorService.ProxyConfig second = service.pickNextProxyOrNull();
        ProxySelectorService.ProxyConfig third = service.pickNextProxyOrNull();

        assertNotNull(first);
        assertNotNull(second);
        assertNotNull(third);
        assertEquals("http://10.0.0.1:8080", first.server());
        assertEquals("user1", first.username());
        assertEquals("pass1", first.password());
        assertEquals("http://10.0.0.2:8080", second.server());
        assertEquals("user2", second.username());
        assertEquals("pass2", second.password());
        assertEquals("http://10.0.0.1:8080", third.server());
        assertEquals("user1", third.username());
        assertEquals("pass1", third.password());
    }

    @Test
    void pickNextProxyOrNullShouldReturnNullCredentialsWhenBlank() {
        ProxiSetting proxyWithoutAuth = new ProxiSetting("http://10.0.0.3:8080", "  ", null, true, 1);

        when(proxiSettingService.listarActivos()).thenReturn(List.of(proxyWithoutAuth));

        ProxySelectorService.ProxyConfig result = service.pickNextProxyOrNull();

        assertNotNull(result);
        assertEquals("http://10.0.0.3:8080", result.server());
        assertNull(result.username());
        assertNull(result.password());
    }
}
