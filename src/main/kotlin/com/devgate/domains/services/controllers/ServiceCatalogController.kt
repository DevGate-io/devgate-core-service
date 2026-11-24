package com.devgate.domains.services.controllers

import com.devgate.domains.audit.models.Action
import com.devgate.domains.audit.services.AuditLogService
import com.devgate.domains.services.dto.CreateServiceRequest
import com.devgate.domains.services.dto.ServiceResponse
import com.devgate.domains.services.dto.UpdateServiceRequest
import com.devgate.domains.services.dto.toTarget
import com.devgate.domains.services.models.enums.ServiceHealth
import com.devgate.domains.services.services.ServiceCatalogService
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
@RequestMapping("/services")
class ServiceCatalogController
	@Autowired
	constructor(
		private val serviceCatalogService: ServiceCatalogService,
		private val auditLogService: AuditLogService
	) {
		@GetMapping
		fun getAll(
			@RequestParam(required = false) search: String?,
			@RequestParam(required = false) tags: List<String>?,
			@RequestParam(required = false) health: String?,
			@RequestParam(required = false) ownerTeamId: String?,
			@RequestParam(required = false) projectId: String?
		): ResponseEntity<List<ServiceResponse>> =
			ResponseEntity.ok(
				serviceCatalogService.getAllServices(
					search = search,
					tags = tags,
					health = parseHealth(health),
					ownerTeamId = parseOptionalUuid(ownerTeamId),
					projectId = parseOptionalUuid(projectId)
				)
			)

		@GetMapping("/tags")
		fun getTags(): ResponseEntity<List<String>> = ResponseEntity.ok(serviceCatalogService.getAllTags())

		@GetMapping("/{id}")
		fun getById(
			@PathVariable id: String
		): ResponseEntity<ServiceResponse> {
			val uuid = parseUuid(id) ?: return ResponseEntity.badRequest().build()
			return ResponseEntity.ok(serviceCatalogService.getServiceById(uuid))
		}

		@PostMapping
		@PreAuthorize("hasRole('ADMIN')")
		fun create(
			@RequestBody @Valid body: CreateServiceRequest
		): ResponseEntity<ServiceResponse> {
			val response = serviceCatalogService.createService(body)
			auditLogService.sendMessage(Action.SERVICE_CREATED, response.toTarget())

			return ResponseEntity.ok(response)
		}

		@PatchMapping("/{id}")
		@PreAuthorize("hasRole('ADMIN')")
		fun update(
			@PathVariable id: String,
			@RequestBody @Valid body: UpdateServiceRequest
		): ResponseEntity<ServiceResponse> {
			val uuid = parseUuid(id) ?: return ResponseEntity.badRequest().build()
			val response = serviceCatalogService.updateService(uuid, body)

			auditLogService.sendMessage(Action.SERVICE_UPDATED, response.toTarget())

			return ResponseEntity.ok(response)
		}

		@DeleteMapping("/{id}")
		@PreAuthorize("hasRole('ADMIN')")
		fun delete(
			@PathVariable id: String
		): ResponseEntity<Nothing> {
			val uuid = parseUuid(id) ?: return ResponseEntity.badRequest().build()
			val service = serviceCatalogService.getServiceById(uuid)

			serviceCatalogService.deleteService(uuid)
			auditLogService.sendMessage(Action.SERVICE_DELETED, service.toTarget())

			return ResponseEntity.ok().build()
		}

		private fun parseUuid(id: String): UUID? = runCatching { UUID.fromString(id) }.getOrNull()

		private fun parseOptionalUuid(id: String?): UUID? = id?.let { parseUuid(it) }

		private fun parseHealth(raw: String?): ServiceHealth? =
			raw?.let { value -> ServiceHealth.entries.find { it.value.equals(value, ignoreCase = true) } }
	}