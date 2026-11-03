package com.back.atlas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Limits on the AI endpoints, which are public and cost money per call (LLM tokens).
 *
 * @param perMinute burst limit per client
 * @param perHour   sustained limit per client
 */
@ConfigurationProperties(prefix = "app.rate-limit.ai")
public record RateLimitProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("5") int perMinute,
        @DefaultValue("30") int perHour
) {
}
