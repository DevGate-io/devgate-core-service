package com.devgate.domains.templates.dto

import jakarta.validation.constraints.NotBlank
import java.util.UUID

data class CreateTemplateRequest(
	@NotBlank
	val name: String,
	@NotBlank
	val slug: String,
	val description: String? = null,
	val tags: List<String> = emptyList(),
	val integrationId: UUID,
	@NotBlank
	val templateOwner: String,
	@NotBlank
	val templateRepo: String
)