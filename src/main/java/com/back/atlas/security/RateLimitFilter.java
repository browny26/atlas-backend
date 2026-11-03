package com.back.atlas.security;

import com.back.atlas.config.RateLimitProperties;
import com.back.atlas.entity.User;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Token-bucket rate limiting (Bucket4j) on the AI endpoints.
 * Clients are identified by user id when logged in, otherwise by IP address.
 * Buckets live in a bounded Caffeine cache, so memory cannot grow without limit.
 * In-memory: with several backend instances a shared store (e.g. Redis) would be needed.
 */
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> LIMITED_PATHS = Set.of(
            "/v1/api/itinerary/generate",
            "/v1/api/itinerary/generate/stream",
            "/v1/api/itinerary/regenerate-day"
    );

    private final RateLimitProperties properties;
    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofHours(1))
            .maximumSize(100_000)
            .build();

    public RateLimitFilter(RateLimitProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !properties.enabled()
                || !"POST".equals(request.getMethod())
                || !LIMITED_PATHS.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Bucket bucket = buckets.get(clientKey(request), key -> newBucket());
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
            chain.doFilter(request, response);
            return;
        }

        long retryAfterSeconds = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()));
        log.info("Rate limit exceeded for {} on {}", clientKey(request), request.getRequestURI());
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("""
                {"status":429,"error":"Too Many Requests","message":"Too many itinerary requests, \
                please retry in %d seconds"}""".formatted(retryAfterSeconds));
    }

    private Bucket newBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(properties.perMinute())
                        .refillGreedy(properties.perMinute(), Duration.ofMinutes(1)).build())
                .addLimit(Bandwidth.builder().capacity(properties.perHour())
                        .refillGreedy(properties.perHour(), Duration.ofHours(1)).build())
                .build();
    }

    static String clientKey(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            return "user:" + user.getId();
        }
        return "ip:" + clientIp(request);
    }

    /**
     * X-Forwarded-For is trusted only when the direct peer is on a private network, i.e. a
     * proxy or load balancer such as Render's. Then its last entry (added by that proxy) is
     * the client; earlier entries can be forged. A client reaching the app directly from the
     * internet has a public address, and its header is ignored.
     */
    static String clientIp(HttpServletRequest request) {
        String peer = request.getRemoteAddr();
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank() && isPrivateNetwork(peer)) {
            String[] hops = forwarded.split(",");
            return hops[hops.length - 1].trim();
        }
        return peer;
    }

    private static boolean isPrivateNetwork(String address) {
        try {
            // getRemoteAddr() is always an IP literal, so no DNS lookup happens here.
            InetAddress ip = InetAddress.getByName(address);
            return ip.isLoopbackAddress() || ip.isSiteLocalAddress()
                    || (ip instanceof Inet6Address && (ip.getAddress()[0] & 0xFE) == 0xFC); // fc00::/7
        } catch (UnknownHostException e) {
            return false;
        }
    }
}
