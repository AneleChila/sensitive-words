package com.anele.sensitivewords;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class OpenApiIntegrationTest {
    @Autowired MockMvc mvc;

    @Test void authenticatedDevelopmentUserCanReadSwaggerAndGeneratedContract() throws Exception {
        mvc.perform(get("/swagger-ui.html")).andExpect(status().isUnauthorized());
        mvc.perform(get("/swagger-ui.html").with(httpBasic("client", "client-demo-only")))
            .andExpect(status().is3xxRedirection());
        mvc.perform(get("/v3/api-docs").with(httpBasic("client", "client-demo-only")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paths['/api/v1/messages/filter'].post").exists())
            .andExpect(jsonPath("$.paths['/api/internal/v1/sensitive-words/import'].post.requestBody.content['multipart/form-data']").exists())
            .andExpect(jsonPath("$.paths['/api/internal/v1/sensitive-words/{id}'].delete.responses['204']").exists())
            .andExpect(jsonPath("$.paths['/api/internal/v1/sensitive-words/{id}'].get.responses['404']").exists())
            .andExpect(jsonPath("$.paths['/api/internal/v1/sensitive-words'].get.parameters[1].name").value("size"))
            .andExpect(jsonPath("$.components.schemas.FilterRequest").exists())
            .andExpect(jsonPath("$.paths['/api/v1/messages/filter'].post.requestBody.content['application/json'].example").exists())
            .andExpect(jsonPath("$.paths['/api/internal/v1/sensitive-words'].post.requestBody.content['application/json'].example.value").value("private phrase"))
            .andExpect(jsonPath("$.components.securitySchemes.basicAuth").exists());
    }
}
