package com.johnnycarreiro.hybridauth.resource.infra.database;

import com.johnnycarreiro.hybridauth.resource.domain.task.Task;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence port for {@link Task} (SDD-002 §6). Tasks are scoped by their parent project, not
 * directly by owner — the service verifies project ownership first, then queries by {@code
 * projectId} (SDD-002 §4 invariant 2).
 */
public interface TaskRepository extends JpaRepository<Task, UUID> {

  List<Task> findByProjectIdOrderByCreatedAtDesc(UUID projectId);
}
