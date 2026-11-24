package com.devgate.domains.templates.controllers

import com.devgate.Constants
import com.devgate.domains.audit.models.Action
import com.devgate.domains.audit.services.AuditLogService
import com.devgate.domains.services.dto.toTarget
import com.devgate.domains.templates.dto.CreateTemplateRequest
import com.devgate.domains.templates.dto.RunTemplateRequest
import com.devgate.domains.templates.dto.RunTemplateResponse
import com.devgate.domains.templates.dto.TemplateResponse
import com.devgate.domains.templates.dto.UpdateTemplateRequest
import com.devgate.domains.templates.dto.toTarget
import com.devgate.domains.templates.services.TemplateService
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
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/templates")
class TemplateController
	@Autowired
	constructor(
		private val templateService: TemplateService,
		private val auditLogService: AuditLogService
	) {
		@GetMapping
		fun getAll(
			@RequestParam(required = false) search: String?
		): ResponseEntity<List<TemplateResponse>> = ResponseEntity.ok(templateService.getAllTemplates(search))

		@GetMapping("/{id}")
		fun getById(
			@PathVariable id: String
		): ResponseEntity<TemplateResponse> {
			val uuid = parseUuid(id) ?: return ResponseEntity.badRequest().build()
			return ResponseEntity.ok(templateService.getTemplateById(uuid))
		}

		@PostMapping
		@PreAuthorize("hasRole('ADMIN')")
		fun create(
			@RequestBody @Valid body: CreateTemplateRequest
		): ResponseEntity<TemplateResponse> = ResponseEntity.ok(templateService.createTemplate(body))

		@PatchMapping("/{id}")
		@PreAuthorize("hasRole('ADMIN')")
		fun update(
			@PathVariable id: String,
			@RequestBody @Valid body: UpdateTemplateRequest
		): ResponseEntity<TemplateResponse> {
			val uuid = parseUuid(id) ?: return ResponseEntity.badRequest().build()
			return ResponseEntity.ok(templateService.updateTemplate(uuid, body))
		}

		@DeleteMapping("/{id}")
		@PreAuthorize("hasRole('ADMIN')")
		fun delete(
			@PathVariable id: String
		): ResponseEntity<Nothing> {
			val uuid = parseUuid(id) ?: return ResponseEntity.badRequest().build()
			templateService.deleteTemplate(uuid)

			return ResponseEntity.ok().build()
		}

		// Self-service scaffolding: any authenticated user may run a template, not just ADMIN —
		// including the catalog Service it creates (created in-process, bypassing the ADMIN-only
		// POST /services). The raw bearer token is forwarded to devgate-integration-service as-is
		// (no separate service-to-service credential), so it is taken directly from the header
		// rather than reconstructed from the parsed JwtTokenValidator claims.
		@PostMapping("/{id}/run")
		fun run(
			@PathVariable id: String,
			@RequestBody @Valid body: RunTemplateRequest,
			@RequestHeader(Constants.AUTH_HEADER) authorizationHeader: String
		): ResponseEntity<RunTemplateResponse> {
			val uuid = parseUuid(id) ?: return ResponseEntity.badRequest().build()
			val response = templateService.runTemplate(uuid, body, authorizationHeader)

			// service.created used to be published by devgate-catalog-service when the scaffolder
			// called it over HTTP; the Service is now created in-process, so it is published here.
			auditLogService.sendMessage(Action.SERVICE_CREATED, response.service.toTarget())
			auditLogService.sendMessage(Action.TEMPLATE_USED, response.toTarget())

			return ResponseEntity.ok(response)
		}

		private fun parseUuid(id: String): UUID? = runCatching { UUID.fromString(id) }.getOrNull()
	}