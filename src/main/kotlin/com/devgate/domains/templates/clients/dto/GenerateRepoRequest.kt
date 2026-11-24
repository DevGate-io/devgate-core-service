package com.devgate.domains.templates.clients.dto

/**
 * Wire contract with devgate-integration-service — keep the shape in sync with
 * com.devgate.domains.integrations.dto.GenerateRepoRequest there
 * (`POST /integrations/{id}/generate-repo`).
 */
data class GenerateRepoRequest(
	val templateOwner: String,
	val templateRepo: String,
	val newRepoName: String,
	val owner: String? = null,
	val private: Boolean,
	val description: String? = null
)