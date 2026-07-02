package com.smartcbwtf.util;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientIpResolverTest {

    @Test
    void resolveHonorsForwardedForFromTrustedProxy() {
        MockHttpServletRequest request = request("10.0.0.5");
        request.addHeader("X-Forwarded-For", "203.0.113.7, 10.0.0.5");

        assertEquals("203.0.113.7", ClientIpResolver.resolve(request));
    }

    @Test
    void resolveHonorsRealIpFromTrustedProxyWhenForwardedForMissing() {
        MockHttpServletRequest request = request("127.0.0.1");
        request.addHeader("X-Real-IP", "203.0.113.8");

        assertEquals("203.0.113.8", ClientIpResolver.resolve(request));
    }

    @Test
    void resolveIgnoresSpoofedForwardedForFromDirectClient() {
        MockHttpServletRequest request = request("198.51.100.10");
        request.addHeader("X-Forwarded-For", "203.0.113.9");

        assertEquals("198.51.100.10", ClientIpResolver.resolve(request));
    }

    @Test
    void resolveFallsBackToUnknownWhenRemoteAddressMissing() {
        MockHttpServletRequest request = request(null);
        request.addHeader("X-Forwarded-For", "203.0.113.10");

        assertEquals("unknown", ClientIpResolver.resolve(request));
    }

    private static MockHttpServletRequest request(String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddr);
        return request;
    }
}
