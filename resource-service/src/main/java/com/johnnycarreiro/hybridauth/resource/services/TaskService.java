package com.johnnycarreiro.hybridauth.resource.services;

import com.johnnycarreiro.hybridauth.resource.domain.project.ProjectNotFoundException;
import com.johnnycarreiro.hybridauth.resource.domain.task.Task;
import com.johnnycarreiro.hybridauth.resource.domain.task.TaskNotFoundException;
import com.johnnycarreiro.hybridauth.resource.domain.task.TaskStatus;
import com.johnnycarreiro.hybridauth.resource.infra.database.TaskRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Task CRUD, scoped through the parent project (SDD-002 §8 F-tasks). Tasks carry no owner of their
 * own — authorization is <em>derived</em>: every operation first proves the caller owns the
 * relevant project (via {@link ProjectService}), so a task is reachable exactly when its project
 * is. A task under an unknown or unowned project is reported as {@link TaskNotFoundException}
 * (404), the same privacy-preserving answer the project layer gives (SDD-002 §5 / §4 invariant 2).
 */
@Service
public class TaskService {

  private final TaskRepository tasks;
  private final ProjectService projects;

  public TaskService(TaskRepository tasks, ProjectService projects) {
    this.tasks = tasks;
    this.projects = projects;
  }

  @Transactional
  public Task create(
      UUID ownerId, UUID projectId, String title, String description, TaskStatus status) {
    requireOwnedProject(ownerId, projectId);
    return tasks.save(Task.create(projectId, title, description, status));
  }

  @Transactional(readOnly = true)
  public List<Task> list(UUID ownerId, UUID projectId) {
    requireOwnedProject(ownerId, projectId);
    return tasks.findByProjectIdOrderByCreatedAtDesc(projectId);
  }

  @Transactional(readOnly = true)
  public Task get(UUID ownerId, UUID taskId) {
    Task task = tasks.findById(taskId).orElseThrow(() -> new TaskNotFoundException(taskId));
    if (!projects.isOwnedBy(ownerId, task.projectId())) {
      // The task exists but belongs to another user's project — answer as "task not found".
      throw new TaskNotFoundException(taskId);
    }
    return task;
  }

  @Transactional
  public Task update(
      UUID ownerId, UUID taskId, String title, String description, TaskStatus status) {
    Task task = get(ownerId, taskId);
    task.edit(title, description, status);
    return task; // managed entity — flush on commit
  }

  @Transactional
  public void delete(UUID ownerId, UUID taskId) {
    Task task = get(ownerId, taskId);
    tasks.delete(task);
  }

  /**
   * Gate project-rooted task routes ({@code /projects/{id}/tasks}) on ownership: an unknown or
   * unowned project is a {@link ProjectNotFoundException} (404) — the project named in the path is
   * the thing the caller cannot see.
   */
  private void requireOwnedProject(UUID ownerId, UUID projectId) {
    if (!projects.isOwnedBy(ownerId, projectId)) {
      throw new ProjectNotFoundException(projectId);
    }
  }
}
