package com.devgate.domains.projects.controllers

import com.devgate.domains.audit.models.Action
import com.devgate.domains.audit.services.AuditLogService
import com.devgate.domains.projects.dto.CreateProjectRequest
import com.devgate.domains.projects.dto.ProjectResponse
import com.devgate.domains.projects.dto.UpdateProjectRequest
import com.devgate.domains.projects.dto.toTarget
import com.devgate.domains.projects.services.ProjectService
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/projects")
class ProjectController
	@Autowired
	constructor(
		private val projectService: ProjectService,
		private val auditLogService: AuditLogService
	) {
		@GetMapping
		fun getAll(
			@RequestParam(required = false) search: String?,
			@RequestParam(required = false) teamId: String?
		): ResponseEntity<List<ProjectResponse>> =
			ResponseEntity.ok(projectService.getAllProjects(search, parseOptionalUuid(teamId)))

		@GetMapping("/{id}")
		fun getById(
			@PathVariable id: String
		): ResponseEntity<ProjectResponse> {
			val uuid = parseUuid(id) ?: return ResponseEntity.badRequest().build()
			return ResponseEntity.ok(projectService.getProjectById(uuid))
		}

		@PostMapping
		@PreAuthorize("hasRole('ADMIN')")
		fun create(
			@RequestBody @Valid body: CreateProjectRequest
		): ResponseEntity<ProjectResponse> {
			val response = projectService.createProject(body)
			auditLogService.sendMessage(Action.PROJECT_CREATED, response.toTarget())

			return ResponseEntity.ok(response)
		}

		@PatchMapping("/{id}")
		@PreAuthorize("hasRole('ADMIN')")
		fun update(
			@PathVariable id: String,
			@RequestBody @Valid body: UpdateProjectRequest
		): ResponseEntity<ProjectResponse> {
			val uuid = parseUuid(id) ?: return ResponseEntity.badRequest().build()
			val response = projectService.updateProject(uuid, body)

			auditLogService.sendMessage(Action.PROJECT_UPDATED, response.toTarget())

			return ResponseEntity.ok(response)
		}

		@DeleteMapping("/{id}")
		@PreAuthorize("hasRole('ADMIN')")
		fun delete(
			@PathVariable id: String
		): ResponseEntity<Nothing> {
			val uuid = parseUuid(id) ?: return ResponseEntity.badRequest().build()
			val project = projectService.getProjectById(uuid)

			projectService.deleteProject(uuid)
			auditLogService.sendMessage(Action.PROJECT_DELETED, project.toTarget())

			return ResponseEntity.ok().build()
		}

		private fun parseUuid(id: String): UUID? = runCatching { UUID.fromString(id) }.getOrNull()

		private fun parseOptionalUuid(id: String?): UUID? = id?.let { parseUuid(it) }
	}