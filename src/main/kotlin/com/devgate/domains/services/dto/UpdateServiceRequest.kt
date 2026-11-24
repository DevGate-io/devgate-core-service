package com.devgate.domains.services.dto

import com.devgate.domains.projects.models.Project
import com.devgate.domains.services.models.Service
import java.time.Instant
import java.util.UUID

data class UpdateServiceRequest(
	val name: String? = null,
	val slug: String? = null,
	val description: String? = null,
	val language: String? = null,
	val ownerTeamId: UUID? = null,
	val projectId: UUID? = null,
	val repoUrl: String? = null,
	val pipelineUrl: String? = null,
	val docsUrl: String? = null,
	val tags: List<String>? = null,
	val environments: List<ServiceEnvironmentDto>? = null
)

fun Service.applyPatch(
	request: UpdateServiceRequest,
	project: Project?
): Service =
	apply {
		request.name?.let { name = it }
		request.slug?.let { slug = it }
		request.description?.let { description = it }
		request.language?.let { language = it }
		request.ownerTeamId?.let { ownerTeamId = it }
		project?.let { this.project = it }
		request.repoUrl?.let { repoUrl = it }
		request.pipelineUrl?.let { pipelineUrl = it }
		request.docsUrl?.let { docsUrl = it }
		request.tags?.let { tags = it.toMutableList() }
		request.environments?.let { envDtos ->
			environments.clear()
			environments.addAll(envDtos.map { it.toEnvironment(this) })
		}
		updatedAt = Instant.now()
	}