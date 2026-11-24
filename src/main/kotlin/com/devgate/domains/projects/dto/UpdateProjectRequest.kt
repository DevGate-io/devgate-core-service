package com.devgate.domains.projects.dto

import java.util.UUID

data class UpdateProjectRequest(
	val name: String? = null,
	val description: String? = null,
	val slug: String? = null,
	val teamId: UUID? = null
)