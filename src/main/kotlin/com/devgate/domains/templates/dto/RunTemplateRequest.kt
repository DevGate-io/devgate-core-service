package com.devgate.domains.templates.dto

import jakarta.validation.constraints.NotBlank
import java.util.UUID

data class RunTemplateRequest(
	@NotBlank
	val newRepoName: String,
	val private: Boolean = true,
	val description: String? = null,
	val ownerTeamId: UUID,
	val projectId: UUID
)