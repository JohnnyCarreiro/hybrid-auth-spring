package com.johnnycarreiro.hybridauth.resource.web;

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

/**
 * Task CRUD with ownership derived through the parent project: happy path + cross-user 404 + 400.
 */
class TaskCrudIntegrationTest extends AbstractResourceIT {

  @Test
  void createsAndManagesTasksUnderAnOwnedProject() throws Exception {
    UUID alice = UUID.randomUUID();
    String project = createProject(alice, "alice@example.com");

    // create — status omitted → defaults to TODO
    String taskBody =
        mockMvc
            .perform(
                post("/projects/" + project + "/tasks")
                    .header(HttpHeaders.AUTHORIZATION, tokens.bearer(alice, "alice@example.com"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"title\":\"Write the SDD\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("TODO"))
            .andExpect(jsonPath("$.projectId").value(project))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String task = objectMapper.readTree(taskBody).get("id").asText();

    // list under the project
    mockMvc
        .perform(
            get("/projects/" + project + "/tasks")
                .header(HttpHeaders.AUTHORIZATION, tokens.bearer(alice, "alice@example.com")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(task));

    // update status
    mockMvc
        .perform(
            put("/tasks/" + task)
                .header(HttpHeaders.AUTHORIZATION, tokens.bearer(alice, "alice@example.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Write the SDD\",\"status\":\"DOING\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("DOING"));

    // delete
    mockMvc
        .perform(
            delete("/tasks/" + task)
                .header(HttpHeaders.AUTHORIZATION, tokens.bearer(alice, "alice@example.com")))
        .andExpect(status().isNoContent());
    mockMvc
        .perform(
            get("/tasks/" + task)
                .header(HttpHeaders.AUTHORIZATION, tokens.bearer(alice, "alice@example.com")))
        .andExpect(status().isNotFound());
  }

  @Test
  void cannotCreateOrReadTasksAcrossOwnershipBoundaries() throws Exception {
    UUID alice = UUID.randomUUID();
    UUID mallory = UUID.randomUUID();
    String project = createProject(alice, "alice@example.com");
    String task =
        objectMapper
            .readTree(
                mockMvc
                    .perform(
                        post("/projects/" + project + "/tasks")
                            .header(
                                HttpHeaders.AUTHORIZATION,
                                tokens.bearer(alice, "alice@example.com"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"private\"}"))
                    .andReturn()
                    .getResponse()
                    .getContentAsString())
            .get("id")
            .asText();

    String malloryToken = tokens.bearer(mallory, "mallory@example.com");

    // mallory cannot create a task under alice's project (project unseen → 404)
    mockMvc
        .perform(
            post("/projects/" + project + "/tasks")
                .header(HttpHeaders.AUTHORIZATION, malloryToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"sneaky\"}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));

    // nor read alice's task
    mockMvc
        .perform(get("/tasks/" + task).header(HttpHeaders.AUTHORIZATION, malloryToken))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("TASK_NOT_FOUND"));
  }

  @Test
  void rejectsAnUnknownStatusWith400() throws Exception {
    UUID alice = UUID.randomUUID();
    String project = createProject(alice, "alice@example.com");
    mockMvc
        .perform(
            post("/projects/" + project + "/tasks")
                .header(HttpHeaders.AUTHORIZATION, tokens.bearer(alice, "alice@example.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"x\",\"status\":\"NONSENSE\"}"))
        .andExpect(status().isBadRequest());
  }

  private String createProject(UUID owner, String email) throws Exception {
    String body =
        mockMvc
            .perform(
                post("/projects")
                    .header(HttpHeaders.AUTHORIZATION, tokens.bearer(owner, email))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Project\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return objectMapper.readTree(body).get("id").asText();
  }
}
