package com.example.veritusbot.service.scraper.browser;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class ProxySelectorServiceTest {

    @Test
    void pickNextProxyOrNullShouldReturnNullWhenNoProxyConfigured() {
        ProxySelectorService service = new ProxySelectorService();

        assertNull(service.pickNextProxyOrNull());
    }

    @Test
    void pickNextProxyOrNullShouldReturnConfiguredProxiesInSequenceAndWrapAround() {
        ProxySelectorService service = new ProxySelectorService();
        ReflectionTestUtils.setField(service, "proxi1", "http://10.0.0.1:8080");
        ReflectionTestUtils.setField(service, "userProxi1", "user1");
        ReflectionTestUtils.setField(service, "passProxi1", "pass1");
        ReflectionTestUtils.setField(service, "proxi2", "http://10.0.0.2:8080");
        ReflectionTestUtils.setField(service, "userProxi2", "user2");
        ReflectionTestUtils.setField(service, "passProxi2", "pass2");
        ReflectionTestUtils.setField(service, "proxi3", " ");

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
}

