package com.anele.sensitivewords.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@Configuration
@Profile("local")
public class LocalUsers {
    @Bean
    UserDetailsService demoUserDetailsService(@Value("${app.security.admin-password}") String adminPassword,
                                 @Value("${app.security.client-password}") String clientPassword) {
        var encoder = new BCryptPasswordEncoder();
        return new InMemoryUserDetailsManager(
            User.withUsername("admin").password(encoder.encode(adminPassword))
                .authorities("SCOPE_words.manage", "SCOPE_messages.filter").build(),
            User.withUsername("client").password(encoder.encode(clientPassword))
                .authorities("SCOPE_messages.filter").build());
    }
    @Bean
    BCryptPasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
}
