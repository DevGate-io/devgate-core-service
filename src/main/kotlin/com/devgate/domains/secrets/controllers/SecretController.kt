package com.devgate.domains.secrets.controllers

import com.devgate.domains.audit.models.Action
import com.devgate.domains.audit.services.AuditLogService
import com.devgate.domains.secrets.dto.CreateSecretRequest
import com.devgate.domains.secrets.dto.SecretResponse
import com.devgate.domains.secrets.dto.UpdateSecretRequest
import com.devgate.domains.secrets.dto.toTarget
import com.devgate.domains.secrets.services.SecretService
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
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/services/{serviceId}/secrets")
class SecretController
	@Autowired
	constructor(
		private val secretService: SecretService,
		private val auditLogService: AuditLogService
	) {
		@GetMapping
		fun getAll(
			@PathVariable serviceId: String
		): ResponseEntity<List<SecretResponse>> {
			val uuid = parseUuid(serviceId) ?: return ResponseEntity.badRequest().build()
			return ResponseEntity.ok(secretService.getAllForService(uuid))
		}

		@GetMapping("/{id}")
		fun getById(
			@PathVariable serviceId: String,
			@PathVariable id: String
		): ResponseEntity<SecretResponse> {
			val serviceUuid = parseUuid(serviceId) ?: return ResponseEntity.badRequest().build()
			val uuid = parseUuid(id) ?: return ResponseEntity.badRequest().build()

			return ResponseEntity.ok(secretService.getSecret(serviceUuid, uuid))
		}

		@PostMapping
		@PreAuthorize("hasRole('ADMIN')")
		fun create(
			@PathVariable serviceId: String,
			@RequestBody @Valid body: CreateSecretRequest
		): ResponseEntity<SecretResponse> {
			val serviceUuid = parseUuid(serviceId) ?: return ResponseEntity.badRequest().build()

			val response = secretService.createSecret(serviceUuid, body)
			auditLogService.sendMessage(Action.SECRET_CREATED, response.toTarget())

			return ResponseEntity.ok(response)
		}

		@PatchMapping("/{id}")
		@PreAuthorize("hasRole('ADMIN')")
		fun update(
			@PathVariable serviceId: String,
			@PathVariable id: String,
			@RequestBody @Valid body: UpdateSecretRequest
		): ResponseEntity<SecretResponse> {
			val serviceUuid = parseUuid(serviceId) ?: return ResponseEntity.badRequest().build()
			val uuid = parseUuid(id) ?: return ResponseEntity.badRequest().build()

			val response = secretService.updateSecret(serviceUuid, uuid, body)
			auditLogService.sendMessage(Action.SECRET_UPDATED, response.toTarget())

			return ResponseEntity.ok(response)
		}

		@DeleteMapping("/{id}")
		@PreAuthorize("hasRole('ADMIN')")
		fun delete(
			@PathVariable serviceId: String,
			@PathVariable id: String
		): ResponseEntity<Nothing> {
			val serviceUuid = parseUuid(serviceId) ?: return ResponseEntity.badRequest().build()
			val uuid = parseUuid(id) ?: return ResponseEntity.badRequest().build()

			val secret = secretService.getSecret(serviceUuid, uuid)
			secretService.deleteSecret(serviceUuid, uuid)
			auditLogService.sendMessage(Action.SECRET_DELETED, secret.toTarget())

			return ResponseEntity.ok().build()
		}

		@PostMapping("/{id}/rotate")
		@PreAuthorize("hasRole('ADMIN')")
		fun rotate(
			@PathVariable serviceId: String,
			@PathVariable id: String
		): ResponseEntity<SecretResponse> {
			val serviceUuid = parseUuid(serviceId) ?: return ResponseEntity.badRequest().build()
			val uuid = parseUuid(id) ?: return ResponseEntity.badRequest().build()

			val response = secretService.rotateSecret(serviceUuid, uuid)
			auditLogService.sendMessage(Action.SECRET_ROTATED, response.toTarget())

			return ResponseEntity.ok(response)
		}

		private fun parseUuid(id: String): UUID? = runCatching { UUID.fromString(id) }.getOrNull()
	}