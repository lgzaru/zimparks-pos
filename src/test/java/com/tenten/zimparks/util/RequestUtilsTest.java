package com.tenten.zimparks.util;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class RequestUtilsTest {

    @Test
    void getClientIp_shouldReturnIpFromXForwardedFor() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1, 10.0.0.1");
        
        assertEquals("192.168.1.1", RequestUtils.getClientIp(request));
    }

    @Test
    void getClientIp_shouldReturnIpFromProxyClientIP() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getHeader("Proxy-Client-IP")).thenReturn("192.168.1.2");
        
        assertEquals("192.168.1.2", RequestUtils.getClientIp(request));
    }

    @Test
    void getClientIp_shouldReturnIpv4LoopbackWhenIpv6Loopback() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("0:0:0:0:0:0:0:1");
        
        assertEquals("127.0.0.1", RequestUtils.getClientIp(request));
    }

    @Test
    void getClientIp_shouldHandleUnknownHeaderValue() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("unknown");
        when(request.getRemoteAddr()).thenReturn("192.168.1.3");
        
        assertEquals("192.168.1.3", RequestUtils.getClientIp(request));
    }

    @Test
    void getClientIp_shouldReturnIpv4LoopbackWhenIpv6LoopbackInHeader() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("0:0:0:0:0:0:0:1");
        
        assertEquals("127.0.0.1", RequestUtils.getClientIp(request));
    }
}
