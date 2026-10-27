package com.anele.sensitivewords.config;

import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    private HttpSecurity common(HttpSecurity http) throws Exception {
        return http.csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers("/api/internal/v1/sensitive-words", "/api/internal/v1/sensitive-words/**")
                    .hasAuthority("SCOPE_words.manage")
                .requestMatchers(HttpMethod.POST, "/api/v1/messages/filter").hasAuthority("SCOPE_messages.filter")
                .anyRequest().denyAll());
    }
    @Bean
    @Profile("local")
    SecurityFilterChain localSecurity(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
            .requestMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").authenticated());
        return common(http).httpBasic(Customizer.withDefaults()).build();
    }
    @Bean
    @Profile("!local")
    SecurityFilterChain productionSecurity(HttpSecurity http) throws Exception {
        return common(http).oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults())).build();
    }
}
