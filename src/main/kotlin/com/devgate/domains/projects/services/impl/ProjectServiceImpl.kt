package com.devgate.domains.projects.services.impl

import com.devgate.domains.projects.dto.CreateProjectRequest
import com.devgate.domains.projects.dto.ProjectResponse
import com.devgate.domains.projects.dto.UpdateProjectRequest
import com.devgate.domains.projects.dto.toResponse
import com.devgate.domains.projects.models.Project
import com.devgate.domains.projects.repositories.ProjectRepository
import com.devgate.domains.projects.services.ProjectService
import com.devgate.exceptions.ProjectAlreadyExistsException
import com.devgate.exceptions.ProjectNotFoundException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class ProjectServiceImpl
	@Autowired
	constructor(
		private val projectRepository: ProjectRepository
	) : ProjectService {
		@Transactional(readOnly = true)
		override fun getAllProjects(
			search: String?,
			teamId: UUID?
		): List<ProjectResponse> {
			val all = projectRepository.findAll()
			val query = search?.trim()?.lowercase().orEmpty()

			return all
				.asSequence()
				.filter { teamId == null || it.teamId == teamId }
				.filter {
					query.isEmpty() ||
						it.name.lowercase().contains(query) ||
						it.slug.lowercase().contains(query) ||
						it.description.lowercase().contains(query)
				}.map { it.toResponse() }
				.toList()
		}

		@Transactional(readOnly = true)
		override fun getProjectById(id: UUID): ProjectResponse = findProject(id).toResponse()

		@Transactional
		override fun createProject(request: CreateProjectRequest): ProjectResponse {
			if (projectRepository.existsBySlug(request.slug)) {
				throw ProjectAlreadyExistsException()
			}

			val project =
				Project(
					teamId = request.teamId,
					name = request.name,
					description = request.description ?: "",
					slug = request.slug
				)

			return projectRepository.save(project).toResponse()
		}

		@Transactional
		override fun updateProject(
			id: UUID,
			request: UpdateProjectRequest
		): ProjectResponse {
			val project = findProject(id)

			request.slug?.let { slug ->
				if (slug != project.slug && projectRepository.existsBySlug(slug)) {
					throw ProjectAlreadyExistsException()
				}
				project.slug = slug
			}
			request.name?.let { project.name = it }
			request.description?.let { project.description = it }
			request.teamId?.let { project.teamId = it }
			project.updatedAt = Instant.now()

			return projectRepository.save(project).toResponse()
		}

		@Transactional
		override fun deleteProject(id: UUID) {
			val project = findProject(id)
			projectRepository.delete(project)
		}

		private fun findProject(id: UUID): Project = projectRepository.findById(id).orElseThrow { ProjectNotFoundException() }
	}