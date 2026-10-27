package com.anele.sensitivewords;

import com.anele.sensitivewords.word.SensitiveWordRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest(properties = {"spring.profiles.active=local", "springdoc.api-docs.enabled=false"})
@AutoConfigureMockMvc
class MssqlIntegrationIT {
    @Container static final MSSQLServerContainer<?> SQL_SERVER = new MSSQLServerContainer<>(
        DockerImageName.parse("mcr.microsoft.com/mssql/server:2022-CU17-ubuntu-22.04")).acceptLicense();

    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", SQL_SERVER::getJdbcUrl);
        registry.add("spring.datasource.username", SQL_SERVER::getUsername);
        registry.add("spring.datasource.password", SQL_SERVER::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired SensitiveWordRepository repository;

    @Test void migrationsCrudDuplicatesImportRollbackAndFilteringWorkOnSqlServer() throws Exception {
        assertThat(repository.count()).isEqualTo(228);
        String created = mvc.perform(post("/api/internal/v1/sensitive-words").with(admin())
                .contentType(MediaType.APPLICATION_JSON).content("{\"value\":\"mssql lifecycle\"}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = new com.fasterxml.jackson.databind.ObjectMapper().readTree(created).get("id").asText();
        mvc.perform(post("/api/internal/v1/sensitive-words").with(admin())
                .contentType(MediaType.APPLICATION_JSON).content("{\"value\":\" MSSQL   LIFECYCLE \"}"))
            .andExpect(status().isConflict());
        filter("mssql lifecycle").andExpect(jsonPath("$.message").value("***** *********"));
        mvc.perform(put("/api/internal/v1/sensitive-words/" + id).with(admin())
                .contentType(MediaType.APPLICATION_JSON).content("{\"value\":\"mssql updated\"}"))
            .andExpect(status().isOk());
        filter("mssql lifecycle mssql updated").andExpect(jsonPath("$.message").value("mssql lifecycle ***** *******"));

        StringBuilder json = new StringBuilder("[");
        for (int index = 0; index < 2_501; index++) {
            if (index > 0) json.append(',');
            json.append('"').append("SQLBULK").append(index).append('"');
        }
        json.append(",\" sqlbulk0 \"]");
        mvc.perform(multipart("/api/internal/v1/sensitive-words/import").file(new MockMultipartFile(
                "file", "bulk.txt", "application/json", json.toString().getBytes())).with(admin()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.received").value(2_502))
            .andExpect(jsonPath("$.inserted").value(2_501)).andExpect(jsonPath("$.skippedDuplicates").value(1));
        filter("sqlbulk2500").andExpect(jsonPath("$.message").value("***********"));
        mvc.perform(multipart("/api/internal/v1/sensitive-words/import").file(new MockMultipartFile(
                "file", "bad.txt", "application/json", "[\"ROLLBACKONLY\",false]".getBytes())).with(admin()))
            .andExpect(status().isBadRequest());
        filter("ROLLBACKONLY").andExpect(jsonPath("$.message").value("ROLLBACKONLY"));
        mvc.perform(delete("/api/internal/v1/sensitive-words/" + id).with(admin())).andExpect(status().isNoContent());
        filter("mssql updated").andExpect(jsonPath("$.message").value("mssql updated"));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor admin() {
        return httpBasic("admin", "admin-demo-only");
    }
    private org.springframework.test.web.servlet.ResultActions filter(String message) throws Exception {
        return mvc.perform(post("/api/v1/messages/filter").with(httpBasic("client", "client-demo-only"))
            .contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"" + message + "\"}"));
    }
}
