package com.devgate.domains.projects.dto

import com.devgate.domains.audit.dto.Target
import com.devgate.domains.projects.models.Project
import java.time.Instant
import java.util.UUID

data class ProjectResponse(
	val id: UUID?,
	val teamId: UUID,
	val name: String,
	val description: String,
	val slug: String,
	val createdAt: Instant,
	val updatedAt: Instant
)

fun ProjectResponse.toTarget(): Target =
	Target(
		type = "project",
		id = id.toString(),
		label = name
	)

fun Project.toResponse(): ProjectResponse =
	ProjectResponse(
		id = id,
		teamId = teamId,
		name = name,
		description = description,
		slug = slug,
		createdAt = createdAt,
		updatedAt = updatedAt
	)