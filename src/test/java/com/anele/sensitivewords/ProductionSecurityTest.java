package com.anele.sensitivewords;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:productionsecurity;MODE=MSSQLServer;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "app.security.issuer=https://issuer.example", "app.security.audience=sensitivewords-api"
})
@AutoConfigureMockMvc
class ProductionSecurityTest {
    @Autowired MockMvc mvc;
    @MockitoBean JwtDecoder jwtDecoder;

    @Test void bearerScopesSeparateClientAndInternalAccess() throws Exception {
        when(jwtDecoder.decode("client-token")).thenReturn(token("client-token", "messages.filter"));
        when(jwtDecoder.decode("admin-token")).thenReturn(token("admin-token", "words.manage"));
        mvc.perform(post("/api/v1/messages/filter").header("Authorization", "Bearer client-token")
            .contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"DROP\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.message").value("****"));
        mvc.perform(get("/api/internal/v1/sensitive-words").header("Authorization", "Bearer client-token"))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/internal/v1/sensitive-words").header("Authorization", "Bearer admin-token"))
            .andExpect(status().isOk());
    }
    @Test void invalidAndMissingBearerTokensAreRejected() throws Exception {
        when(jwtDecoder.decode("bad-token")).thenThrow(new BadJwtException("Invalid token"));
        mvc.perform(get("/api/internal/v1/sensitive-words").header("Authorization", "Bearer bad-token"))
            .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/internal/v1/sensitive-words")).andExpect(status().isUnauthorized());
    }
    @Test void demoBasicCredentialsAreNotEnabledInProduction() throws Exception {
        mvc.perform(get("/api/internal/v1/sensitive-words").header("Authorization",
            "Basic " + java.util.Base64.getEncoder().encodeToString("admin:admin-demo-only".getBytes(java.nio.charset.StandardCharsets.UTF_8))))
            .andExpect(status().isUnauthorized());
    }
    private Jwt token(String value, String scope) {
        return Jwt.withTokenValue(value).header("alg", "RS256").subject("test-caller")
            .issuer("https://issuer.example").audience(java.util.List.of("sensitivewords-api"))
            .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300)).claim("scope", scope).build();
    }
}
