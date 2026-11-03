package com.back.atlas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Connection settings for the Python AI microservice (atlas-ai_service).
 *
 * @param url     base URL of the service, e.g. https://atlas-ai-service.onrender.com
 * @param apiKey  shared secret sent in the X-API-Key header
 * @param timeout read timeout: LLM generation (with retries) can take several seconds
 */
@ConfigurationProperties(prefix = "ai.service")
public record AiServiceProperties(
        String url,
        String apiKey,
        @DefaultValue("90s") Duration timeout
) {
}
