package com.devgate.domains.templates.dto

import java.util.UUID

data class UpdateTemplateRequest(
	val name: String? = null,
	val slug: String? = null,
	val description: String? = null,
	val tags: List<String>? = null,
	val integrationId: UUID? = null,
	val templateOwner: String? = null,
	val templateRepo: String? = null
)