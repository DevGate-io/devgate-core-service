package com.devgate.domains.projects.dto

import jakarta.validation.constraints.NotBlank
import java.util.UUID

data class CreateProjectRequest(
	@NotBlank
	val name: String,
	@NotBlank
	val slug: String,
	val description: String? = null,
	val teamId: UUID
)