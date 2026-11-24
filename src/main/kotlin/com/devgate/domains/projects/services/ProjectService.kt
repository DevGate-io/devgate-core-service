package com.devgate.domains.projects.services

import com.devgate.domains.projects.dto.CreateProjectRequest
import com.devgate.domains.projects.dto.ProjectResponse
import com.devgate.domains.projects.dto.UpdateProjectRequest
import java.util.UUID

interface ProjectService {
	fun getAllProjects(
		search: String? = null,
		teamId: UUID? = null
	): List<ProjectResponse>

	fun getProjectById(id: UUID): ProjectResponse

	fun createProject(request: CreateProjectRequest): ProjectResponse

	fun updateProject(
		id: UUID,
		request: UpdateProjectRequest
	): ProjectResponse

	fun deleteProject(id: UUID)
}