package com.example.veritusbot.service.scraper.browser;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class ProxySelectorServiceTest {

    @Test
    void pickRandomProxyOrNullShouldReturnNullWhenNoProxyConfigured() {
        ProxySelectorService service = new ProxySelectorService();

        assertNull(service.pickRandomProxyOrNull());
    }

    @Test
    void pickRandomProxyOrNullShouldReturnConfiguredProxy() {
        ProxySelectorService service = new ProxySelectorService();
        ReflectionTestUtils.setField(service, "proxi1", "http://10.0.0.1:8080");
        ReflectionTestUtils.setField(service, "userProxi1", "user1");
        ReflectionTestUtils.setField(service, "passProxi1", "pass1");
        ReflectionTestUtils.setField(service, "proxi2", "http://10.0.0.2:8080");
        ReflectionTestUtils.setField(service, "userProxi2", "user2");
        ReflectionTestUtils.setField(service, "passProxi2", "pass2");
        ReflectionTestUtils.setField(service, "proxi3", " ");

        ProxySelectorService.ProxyConfig selected = service.pickRandomProxyOrNull();

        assertNotNull(selected);
        assertTrue(
                selected.server().equals("http://10.0.0.1:8080") || selected.server().equals("http://10.0.0.2:8080"),
                "Selected proxy must be one of configured values"
        );
        assertNotNull(selected.username());
        assertNotNull(selected.password());
    }
}

