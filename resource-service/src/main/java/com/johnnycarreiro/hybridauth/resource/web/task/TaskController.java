package com.johnnycarreiro.hybridauth.resource.web.task;

import com.johnnycarreiro.hybridauth.resource.domain.task.Task;
import com.johnnycarreiro.hybridauth.resource.services.CurrentUser;
import com.johnnycarreiro.hybridauth.resource.services.TaskService;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * Owner-scoped CRUD over tasks (SRS+SAD §1.4 / SDD-002 §7). Tasks are nested under a project for
 * list/create ({@code /projects/{projectId}/tasks}) and addressed directly for read/update/delete
 * ({@code /tasks/{id}}). Ownership is derived through the parent project in {@link TaskService}
 * (404 on another user's project/task).
 */
@RestController
public class TaskController {

  private final TaskService tasks;
  private final CurrentUser currentUser;

  public TaskController(TaskService tasks, CurrentUser currentUser) {
    this.tasks = tasks;
    this.currentUser = currentUser;
  }

  @GetMapping("/projects/{projectId}/tasks")
  public List<TaskResponse> list(@PathVariable UUID projectId) {
    return tasks.list(currentUser.requireId(), projectId).stream().map(TaskResponse::from).toList();
  }

  @PostMapping("/projects/{projectId}/tasks")
  public ResponseEntity<TaskResponse> create(
      @PathVariable UUID projectId, @Valid @RequestBody TaskRequest request) {
    Task task =
        tasks.create(
            currentUser.requireId(),
            projectId,
            request.title(),
            request.description(),
            request.status());
    return ResponseEntity.status(HttpStatus.CREATED).body(TaskResponse.from(task));
  }

  @GetMapping("/tasks/{id}")
  public TaskResponse get(@PathVariable UUID id) {
    return TaskResponse.from(tasks.get(currentUser.requireId(), id));
  }

  @PutMapping("/tasks/{id}")
  public TaskResponse update(@PathVariable UUID id, @Valid @RequestBody TaskRequest request) {
    return TaskResponse.from(
        tasks.update(
            currentUser.requireId(), id, request.title(), request.description(), request.status()));
  }

  @DeleteMapping("/tasks/{id}")
  public ResponseEntity<Void> delete(@PathVariable UUID id) {
    tasks.delete(currentUser.requireId(), id);
    return ResponseEntity.noContent().build();
  }
}
