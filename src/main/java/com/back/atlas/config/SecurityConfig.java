package com.back.atlas.config;

import com.back.atlas.security.RateLimitFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(RateLimitProperties.class)
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtFilter;
    private final CorsConfigurationSource corsConfigurationSource;
    private final RateLimitProperties rateLimitProperties;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Stateless JWT API: no cookies/session, so CSRF protection is not needed.
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/v1/api/auth/**", "/error").permitAll()
                        // Airport search feeds the public flight form; activities need login.
                        .requestMatchers(HttpMethod.GET, "/v1/api/airports").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // Generation is public (guests can try the planner, rate limited);
                        // saving requires login.
                        .requestMatchers(HttpMethod.POST,
                                "/v1/api/itinerary/generate",
                                "/v1/api/itinerary/generate/stream",
                                "/v1/api/itinerary/regenerate-day").permitAll()
                        .anyRequest().authenticated()
                )
                // 401 (not 403) when the token is missing: the frontend redirects to sign-in.
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                // After the JWT filter, so logged-in users are limited by id rather than by IP.
                // Created here (not as a @Bean) so it is not also registered as a servlet filter.
                .addFilterAfter(new RateLimitFilter(rateLimitProperties), JwtAuthenticationFilter.class);

        return http.build();
    }
}
