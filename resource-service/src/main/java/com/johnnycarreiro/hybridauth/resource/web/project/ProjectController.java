package com.johnnycarreiro.hybridauth.resource.web.project;

import com.johnnycarreiro.hybridauth.resource.domain.project.Project;
import com.johnnycarreiro.hybridauth.resource.services.CurrentUser;
import com.johnnycarreiro.hybridauth.resource.services.ProjectService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Owner-scoped CRUD over {@code /projects} (SRS+SAD §1.4 / SDD-002 §7). The owner is the verified
 * token {@code sub}, read from {@link CurrentUser}; the service enforces ownership (404 on another
 * user's project). Standard Spring MVC — the auth is the interesting part, the CRUD is deliberately
 * plain.
 */
@RestController
@RequestMapping("/projects")
public class ProjectController {

  private final ProjectService projects;
  private final CurrentUser currentUser;

  public ProjectController(ProjectService projects, CurrentUser currentUser) {
    this.projects = projects;
    this.currentUser = currentUser;
  }

  @GetMapping
  public List<ProjectResponse> list() {
    return projects.list(currentUser.requireId()).stream().map(ProjectResponse::from).toList();
  }

  @PostMapping
  public ResponseEntity<ProjectResponse> create(@Valid @RequestBody ProjectRequest request) {
    Project project =
        projects.create(currentUser.requireId(), request.name(), request.description());
    return ResponseEntity.status(HttpStatus.CREATED).body(ProjectResponse.from(project));
  }

  @GetMapping("/{id}")
  public ProjectResponse get(@PathVariable UUID id) {
    return ProjectResponse.from(projects.get(currentUser.requireId(), id));
  }

  @PutMapping("/{id}")
  public ProjectResponse update(@PathVariable UUID id, @Valid @RequestBody ProjectRequest request) {
    return ProjectResponse.from(
        projects.update(currentUser.requireId(), id, request.name(), request.description()));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable UUID id) {
    projects.delete(currentUser.requireId(), id);
    return ResponseEntity.noContent().build();
  }
}
