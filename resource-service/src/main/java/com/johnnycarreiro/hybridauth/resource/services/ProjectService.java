package com.johnnycarreiro.hybridauth.resource.services;

import com.johnnycarreiro.hybridauth.resource.domain.project.Project;
import com.johnnycarreiro.hybridauth.resource.domain.project.ProjectNotFoundException;
import com.johnnycarreiro.hybridauth.resource.infra.database.ProjectRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Project CRUD, every operation <strong>owner-scoped</strong> (SDD-002 §8 F-projects). The caller's
 * id ({@code ownerId}) is the verified token {@code sub}; it is threaded into every query so an
 * ownership check can never be skipped. A project that does not exist <em>or</em> is owned by
 * someone else is reported identically as {@link ProjectNotFoundException} (404) — the API never
 * confirms another user's data (SDD-002 §5).
 */
@Service
public class ProjectService {

  private final ProjectRepository projects;

  public ProjectService(ProjectRepository projects) {
    this.projects = projects;
  }

  @Transactional
  public Project create(UUID ownerId, String name, String description) {
    return projects.save(Project.create(ownerId, name, description));
  }

  @Transactional(readOnly = true)
  public List<Project> list(UUID ownerId) {
    return projects.findByOwnerIdOrderByCreatedAtDesc(ownerId);
  }

  @Transactional(readOnly = true)
  public Project get(UUID ownerId, UUID id) {
    return projects
        .findByIdAndOwnerId(id, ownerId)
        .orElseThrow(() -> new ProjectNotFoundException(id));
  }

  @Transactional
  public Project update(UUID ownerId, UUID id, String name, String description) {
    Project project = get(ownerId, id);
    project.edit(name, description);
    return project; // managed entity — flush on commit
  }

  @Transactional
  public void delete(UUID ownerId, UUID id) {
    Project project = get(ownerId, id);
    projects.delete(project);
  }

  /**
   * Whether {@code ownerId} owns project {@code id} — used by {@code TaskService} to gate task ops.
   */
  @Transactional(readOnly = true)
  public boolean isOwnedBy(UUID ownerId, UUID id) {
    return projects.existsByIdAndOwnerId(id, ownerId);
  }
}
