package com.johnnycarreiro.hybridauth.resource.web.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Inbound body for creating/updating a project. Edge validation only
 * ({@code @NotBlank}/{@code @Size} → 400 before the use case runs); ownership and persistence rules
 * live in the service.
 */
public record ProjectRequest(
    @NotBlank @Size(max = 200) String name, @Size(max = 2000) String description) {}
