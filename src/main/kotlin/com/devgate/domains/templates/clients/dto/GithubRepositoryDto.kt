package com.devgate.domains.templates.clients.dto

/**
 * Wire contract with devgate-integration-service — keep the shape in sync with
 * com.devgate.domains.integrations.dto.GithubRepositoryDto there.
 */
data class GithubRepositoryDto(
	val name: String,
	val fullName: String,
	val private: Boolean,
	val htmlUrl: String,
	val description: String?,
	val defaultBranch: String,
	val updatedAt: String?,
	val isTemplate: Boolean
)