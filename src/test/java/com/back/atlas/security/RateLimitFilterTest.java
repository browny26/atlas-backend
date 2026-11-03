package com.back.atlas.security;

import com.back.atlas.config.RateLimitProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

    private final RateLimitFilter filter = new RateLimitFilter(new RateLimitProperties(true, 2, 10));

    private MockHttpServletResponse call(String path, String ip) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setRemoteAddr(ip);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    @Test
    void blocksAfterBurstLimitWith429AndRetryAfter() throws Exception {
        assertThat(call("/v1/api/itinerary/generate", "1.1.1.1").getStatus()).isEqualTo(200);
        assertThat(call("/v1/api/itinerary/generate", "1.1.1.1").getStatus()).isEqualTo(200);

        MockHttpServletResponse blocked = call("/v1/api/itinerary/generate", "1.1.1.1");

        assertThat(blocked.getStatus()).isEqualTo(429);
        assertThat(blocked.getHeader("Retry-After")).isNotBlank();
        assertThat(blocked.getContentAsString()).contains("\"status\":429");
    }

    @Test
    void clientsHaveSeparateBuckets() throws Exception {
        call("/v1/api/itinerary/generate", "1.1.1.1");
        call("/v1/api/itinerary/generate", "1.1.1.1");

        assertThat(call("/v1/api/itinerary/generate", "2.2.2.2").getStatus()).isEqualTo(200);
    }

    @Test
    void otherEndpointsAreNotLimited() throws Exception {
        for (int i = 0; i < 5; i++) {
            assertThat(call("/v1/api/itinerary/user", "1.1.1.1").getStatus()).isEqualTo(200);
        }
    }

    @Test
    void behindAPrivateProxyUsesTheLastForwardedForHop() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.5"); // e.g. Render's load balancer
        request.addHeader("X-Forwarded-For", "6.6.6.6, 3.3.3.3");

        assertThat(RateLimitFilter.clientIp(request)).isEqualTo("3.3.3.3");
    }

    @Test
    void directClientsCannotSpoofTheirAddress() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.7"); // public address: not a proxy
        request.addHeader("X-Forwarded-For", "1.2.3.4");

        assertThat(RateLimitFilter.clientIp(request)).isEqualTo("203.0.113.7");

        // A different forged header on every request still hits the same bucket.
        for (int i = 0; i < 2; i++) {
            assertThat(callWithForgedHeader("9.9.9." + i).getStatus()).isEqualTo(200);
        }
        assertThat(callWithForgedHeader("9.9.9.99").getStatus()).isEqualTo(429);
    }

    private MockHttpServletResponse callWithForgedHeader(String forged) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/v1/api/itinerary/generate");
        request.setRemoteAddr("198.51.100.20");
        request.addHeader("X-Forwarded-For", forged);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
