package com.devgate.domains.secrets.dto

import com.devgate.domains.secrets.models.Secret
import com.devgate.domains.secrets.models.enums.SecretProvider
import com.devgate.domains.services.models.enums.ServiceEnvironmentName
import java.time.Instant

data class UpdateSecretRequest(
	val name: String? = null,
	val provider: SecretProvider? = null,
	val reference: String? = null,
	val environment: ServiceEnvironmentName? = null,
	val description: String? = null
)

fun Secret.applyPatch(request: UpdateSecretRequest): Secret =
	apply {
		request.name?.let { name = it }
		request.provider?.let { provider = it }
		request.reference?.let { reference = it }
		request.environment?.let { environment = it }
		request.description?.let { description = it }
		updatedAt = Instant.now()
	}