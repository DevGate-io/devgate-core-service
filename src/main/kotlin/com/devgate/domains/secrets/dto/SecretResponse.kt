package com.devgate.domains.secrets.dto

import com.devgate.domains.audit.dto.Target
import com.devgate.domains.secrets.models.Secret
import com.devgate.domains.secrets.models.enums.SecretProvider
import com.devgate.domains.services.models.enums.ServiceEnvironmentName
import com.fasterxml.jackson.annotation.JsonInclude
import java.time.Instant
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
data class SecretResponse(
	val id: UUID?,
	val serviceId: UUID,
	val name: String,
	val provider: SecretProvider,
	val reference: String,
	val environment: ServiceEnvironmentName?,
	val description: String?,
	val lastRotatedAt: Instant?,
	val createdAt: Instant,
	val updatedAt: Instant
)

fun SecretResponse.toTarget(): Target =
	Target(
		type = "secret",
		id = id.toString(),
		label = name
	)

fun Secret.toResponse(): SecretResponse =
	SecretResponse(
		id = id,
		serviceId = requireNotNull(service.id) { "Service must be persisted before mapping a Secret" },
		name = name,
		provider = provider,
		reference = reference,
		environment = environment,
		description = description,
		lastRotatedAt = lastRotatedAt,
		createdAt = createdAt,
		updatedAt = updatedAt
	)