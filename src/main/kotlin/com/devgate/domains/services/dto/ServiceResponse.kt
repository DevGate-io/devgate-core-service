package com.devgate.domains.services.dto

import com.devgate.domains.audit.dto.Target
import com.devgate.domains.services.models.Service
import com.devgate.domains.services.models.enums.ServiceHealth
import com.fasterxml.jackson.annotation.JsonInclude
import java.time.Instant
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ServiceResponse(
	val id: UUID?,
	val projectId: UUID,
	val ownerTeamId: UUID,
	val name: String,
	val slug: String,
	val description: String,
	val tags: List<String>,
	val language: String?,
	val repoUrl: String?,
	val pipelineUrl: String?,
	val docsUrl: String?,
	val environments: List<ServiceEnvironmentDto>,
	val slo: ServiceSloDto?,
	val health: ServiceHealth,
	val dependencies: List<String>,
	val createdAt: Instant,
	val updatedAt: Instant
)

fun ServiceResponse.toTarget(): Target =
	Target(
		type = "service",
		id = id.toString(),
		label = name
	)

fun Service.toResponse(): ServiceResponse =
	ServiceResponse(
		id = id,
		projectId = requireNotNull(project.id) { "Project must be persisted before mapping a Service" },
		ownerTeamId = ownerTeamId,
		name = name,
		slug = slug,
		description = description,
		tags = tags.toList(),
		language = language,
		repoUrl = repoUrl,
		pipelineUrl = pipelineUrl,
		docsUrl = docsUrl,
		environments = environments.map { it.toDto() },
		slo = sloAvailability?.let { ServiceSloDto(availability = it, latencyP95Ms = sloLatencyP95Ms) },
		health = health,
		dependencies = dependencies.toList(),
		createdAt = createdAt,
		updatedAt = updatedAt
	)