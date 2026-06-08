package com.johnnycarreiro.hybridauth.resource.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.johnnycarreiro.hybridauth.resource.support.AbstractResourceIT;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/** Owner-scoped project CRUD: happy path + cross-user denial (404) + the unauthenticated 401. */
class ProjectCrudIntegrationTest extends AbstractResourceIT {

  @Test
  void createsListsGetsUpdatesAndDeletesItsOwnProjects() throws Exception {
    UUID alice = UUID.randomUUID();

    String id = createProject(alice, "alice@example.com", "Q3 Roadmap", "planning");

    // list returns the owner's project
    mockMvc
        .perform(
            get("/projects")
                .header(HttpHeaders.AUTHORIZATION, tokens.bearer(alice, "alice@example.com")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(id))
        .andExpect(jsonPath("$[0].name").value("Q3 Roadmap"))
        .andExpect(jsonPath("$[0].ownerId").value(alice.toString()));

    // get by id
    mockMvc
        .perform(
            get("/projects/" + id)
                .header(HttpHeaders.AUTHORIZATION, tokens.bearer(alice, "alice@example.com")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.description").value("planning"));

    // update
    mockMvc
        .perform(
            put("/projects/" + id)
                .header(HttpHeaders.AUTHORIZATION, tokens.bearer(alice, "alice@example.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Q3 Roadmap (rev)\",\"description\":\"locked\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Q3 Roadmap (rev)"));

    // delete, then it's gone
    mockMvc
        .perform(
            delete("/projects/" + id)
                .header(HttpHeaders.AUTHORIZATION, tokens.bearer(alice, "alice@example.com")))
        .andExpect(status().isNoContent());
    mockMvc
        .perform(
            get("/projects/" + id)
                .header(HttpHeaders.AUTHORIZATION, tokens.bearer(alice, "alice@example.com")))
        .andExpect(status().isNotFound());
  }

  @Test
  void deniesAccessToAnotherUsersProjectAsNotFound() throws Exception {
    UUID alice = UUID.randomUUID();
    UUID mallory = UUID.randomUUID();
    String id = createProject(alice, "alice@example.com", "Secret", "shh");

    String malloryToken = tokens.bearer(mallory, "mallory@example.com");

    mockMvc
        .perform(get("/projects/" + id).header(HttpHeaders.AUTHORIZATION, malloryToken))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));
    mockMvc
        .perform(
            put("/projects/" + id)
                .header(HttpHeaders.AUTHORIZATION, malloryToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"pwned\"}"))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(delete("/projects/" + id).header(HttpHeaders.AUTHORIZATION, malloryToken))
        .andExpect(status().isNotFound());
    // mallory's own list is empty — she can't see alice's project
    mockMvc
        .perform(get("/projects").header(HttpHeaders.AUTHORIZATION, malloryToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void rejectsUnauthenticatedRequestsWith401() throws Exception {
    mockMvc.perform(get("/projects")).andExpect(status().isUnauthorized());
    mockMvc
        .perform(
            post("/projects").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"x\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void rejectsAnExpiredTokenWith401() throws Exception {
    UUID alice = UUID.randomUUID();
    mockMvc
        .perform(
            get("/projects")
                .header(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer " + tokens.mintExpired(alice, "alice@example.com")))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void rejectsABlankNameWith400() throws Exception {
    UUID alice = UUID.randomUUID();
    mockMvc
        .perform(
            post("/projects")
                .header(HttpHeaders.AUTHORIZATION, tokens.bearer(alice, "alice@example.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"  \"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
  }

  /** Create a project for {@code owner} and return its id. */
  private String createProject(UUID owner, String email, String name, String description)
      throws Exception {
    String body =
        mockMvc
            .perform(
                post("/projects")
                    .header(HttpHeaders.AUTHORIZATION, tokens.bearer(owner, email))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"" + name + "\",\"description\":\"" + description + "\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String id = objectMapper.readTree(body).get("id").asText();
    assertThat(id).isNotBlank();
    return id;
  }
}
