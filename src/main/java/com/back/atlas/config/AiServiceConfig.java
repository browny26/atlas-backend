package com.back.atlas.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(AiServiceProperties.class)
public class AiServiceConfig {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);

    @Bean
    public RestClient aiServiceRestClient(AiServiceProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(properties.timeout());

        return configure(RestClient.builder(), properties)
                .requestFactory(requestFactory)
                .build();
    }

    /** The Python service speaks snake_case JSON; Java records stay camelCase. */
    public static ObjectMapper snakeCaseMapper() {
        return JsonMapper.builder()
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }

    /** Shared by the bean above and by tests (which bind a mock server to the builder). */
    public static RestClient.Builder configure(RestClient.Builder builder, AiServiceProperties properties) {
        ObjectMapper mapper = snakeCaseMapper();
        return builder
                .baseUrl(properties.url())
                .defaultHeader("X-API-Key", properties.apiKey())
                .messageConverters(converters -> {
                    converters.clear();
                    converters.add(new MappingJackson2HttpMessageConverter(mapper));
                });
    }
}
