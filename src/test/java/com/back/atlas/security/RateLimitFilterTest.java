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
    void usesTheLastForwardedForHop() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "6.6.6.6, 3.3.3.3");

        assertThat(RateLimitFilter.clientIp(request)).isEqualTo("3.3.3.3");
    }
}
