package com.anele.sensitivewords;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.mock.web.MockMultipartFile;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class ApiIntegrationTest {
    private static final String WORDS = "/api/internal/v1/sensitive-words";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test void seededPhraseIsFiltered() throws Exception {
        filter("SELECT * FROM customers; DROP TABLE customers")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("****** * **** customers; **** ***** customers"));
    }
    @Test void crudLifecycleImmediatelyChangesFiltering() throws Exception {
        String body = mvc.perform(post(WORDS).with(httpBasic("admin", "admin-demo-only"))
            .contentType(MediaType.APPLICATION_JSON).content("{\"value\":\"  zebraterm  \"}"))
            .andExpect(status().isCreated()).andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.value").value("ZEBRATERM")).andReturn().getResponse().getContentAsString();
        String id = mapper.readTree(body).get("id").asText();
        mvc.perform(get(WORDS + "/" + id).with(httpBasic("admin", "admin-demo-only")))
            .andExpect(status().isOk());
        filter("ZebraTerm hello").andExpect(jsonPath("$.message").value("********* hello"));
        mvc.perform(put(WORDS + "/" + id).with(httpBasic("admin", "admin-demo-only"))
            .contentType(MediaType.APPLICATION_JSON).content("{\"value\":\"zebra phrase\"}"))
            .andExpect(status().isOk());
        filter("zebraterm zebra phrase").andExpect(jsonPath("$.message").value("zebraterm ***** ******"));
        mvc.perform(delete(WORDS + "/" + id).with(httpBasic("admin", "admin-demo-only")))
            .andExpect(status().isNoContent());
        filter("zebra phrase").andExpect(jsonPath("$.message").value("zebra phrase"));
        mvc.perform(get(WORDS + "/" + id).with(httpBasic("admin", "admin-demo-only")))
            .andExpect(status().isNotFound());
    }
    @Test void duplicateIsCaseInsensitive() throws Exception {
        mvc.perform(post(WORDS).with(httpBasic("admin", "admin-demo-only"))
            .contentType(MediaType.APPLICATION_JSON).content("{\"value\":\" select \"}"))
            .andExpect(status().isConflict());
        mvc.perform(post(WORDS).with(httpBasic("admin", "admin-demo-only"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(java.util.Map.of("value", "\u00a0select\u00a0"))))
            .andExpect(status().isConflict());
    }
    @Test void invalidRequestsAreRejected() throws Exception {
        mvc.perform(post(WORDS).with(httpBasic("admin", "admin-demo-only"))
            .contentType(MediaType.APPLICATION_JSON).content("{\"value\":\"   \"}"))
            .andExpect(status().isBadRequest());
        mvc.perform(get(WORDS + "?size=101").with(httpBasic("admin", "admin-demo-only")))
            .andExpect(status().isBadRequest());
        mvc.perform(get(WORDS + "/not-a-uuid").with(httpBasic("admin", "admin-demo-only")))
            .andExpect(status().isBadRequest());
        mvc.perform(put(WORDS + "/" + UUID.randomUUID()).with(httpBasic("admin", "admin-demo-only"))
            .contentType(MediaType.APPLICATION_JSON).content("{\"value\":\"valid\"}"))
            .andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/messages/filter").with(httpBasic("client", "client-demo-only"))
            .contentType(MediaType.APPLICATION_JSON).content("{\"message\":null}"))
            .andExpect(status().isBadRequest());
        filter("x".repeat(10001)).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/messages/filter").with(httpBasic("client", "client-demo-only"))
            .contentType(MediaType.APPLICATION_JSON).content("{"))
            .andExpect(status().isBadRequest());
    }
    @Test void emptyMessageIsAccepted() throws Exception {
        filter("").andExpect(status().isOk()).andExpect(jsonPath("$.message").value(""));
    }
    @Test void importIsAtomicAndReconcilesDuplicates() throws Exception {
        var file = new MockMultipartFile("file", "words.txt", "application/json",
            "[\"new phrase\",\" NEW   PHRASE \",\"SELECT\"]".getBytes());
        mvc.perform(multipart(WORDS + "/import").file(file).with(httpBasic("admin", "admin-demo-only")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.received").value(3))
            .andExpect(jsonPath("$.inserted").value(1)).andExpect(jsonPath("$.skippedDuplicates").value(2));
        filter("new phrase").andExpect(jsonPath("$.message").value("*** ******"));

        var invalid = new MockMultipartFile("file", "bad.txt", "application/json",
            "[\"rollbackmarker\", 7]".getBytes());
        mvc.perform(multipart(WORDS + "/import").file(invalid).with(httpBasic("admin", "admin-demo-only")))
            .andExpect(status().isBadRequest());
        filter("rollbackmarker").andExpect(jsonPath("$.message").value("rollbackmarker"));
    }
    @Test void internalEndpointsAreRestricted() throws Exception {
        mvc.perform(get(WORDS)).andExpect(status().isUnauthorized());
        mvc.perform(get(WORDS).with(httpBasic("client", "client-demo-only")))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/messages/filter").contentType(MediaType.APPLICATION_JSON)
            .content("{\"message\":\"hello\"}")).andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
    }
    @Test void listIsBoundedAndSorted() throws Exception {
        String body = mvc.perform(get(WORDS + "?size=2").with(httpBasic("admin", "admin-demo-only")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(2))
            .andReturn().getResponse().getContentAsString();
        var items = mapper.readTree(body).get("content");
        assertThat(items.get(0).get("value").asText()).isLessThan(items.get(1).get("value").asText());
    }
    private org.springframework.test.web.servlet.ResultActions filter(String message) throws Exception {
        return mvc.perform(post("/api/v1/messages/filter").with(httpBasic("client", "client-demo-only"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(java.util.Map.of("message", message))));
    }
}
