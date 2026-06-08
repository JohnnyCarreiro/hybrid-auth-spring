package com.johnnycarreiro.hybridauth.resource.infra.database;

import com.johnnycarreiro.hybridauth.resource.domain.project.Project;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence port for {@link Project} (SDD-002 §6). Every finder is <strong>owner-scoped</strong>:
 * there is deliberately no plain {@code findById} in the service path, so an ownership check can
 * never be forgotten — the owner is part of the query (SDD-002 §4 invariant 1).
 */
public interface ProjectRepository extends JpaRepository<Project, UUID> {

  List<Project> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

  Optional<Project> findByIdAndOwnerId(UUID id, UUID ownerId);

  boolean existsByIdAndOwnerId(UUID id, UUID ownerId);
}
