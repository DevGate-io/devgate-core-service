package com.devgate.domains.services.dto

import com.devgate.domains.projects.models.Project
import com.devgate.domains.services.models.Service
import jakarta.validation.constraints.NotBlank
import java.util.UUID

data class CreateServiceRequest(
	@NotBlank
	val name: String,
	@NotBlank
	val slug: String,
	val description: String? = null,
	val language: String? = null,
	val ownerTeamId: UUID,
	val projectId: UUID,
	val repoUrl: String? = null,
	val pipelineUrl: String? = null,
	val docsUrl: String? = null,
	val tags: List<String> = emptyList(),
	val environments: List<ServiceEnvironmentDto> = emptyList()
)

fun CreateServiceRequest.toService(project: Project): Service {
	val service =
		Service(
			project = project,
			ownerTeamId = ownerTeamId,
			name = name,
			slug = slug,
			description = description ?: "",
			language = language,
			repoUrl = repoUrl,
			pipelineUrl = pipelineUrl,
			docsUrl = docsUrl
		)

	service.tags = tags.toMutableList()
	service.environments = environments.map { it.toEnvironment(service) }.toMutableList()

	return service
}