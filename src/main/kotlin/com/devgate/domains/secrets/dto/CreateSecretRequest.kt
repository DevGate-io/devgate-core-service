package com.devgate.domains.secrets.dto

import com.devgate.domains.secrets.models.Secret
import com.devgate.domains.secrets.models.enums.SecretProvider
import com.devgate.domains.services.models.Service
import com.devgate.domains.services.models.enums.ServiceEnvironmentName
import jakarta.validation.constraints.NotBlank

data class CreateSecretRequest(
	@NotBlank
	val name: String,
	val provider: SecretProvider,
	@NotBlank
	val reference: String,
	val environment: ServiceEnvironmentName? = null,
	val description: String? = null
)

fun CreateSecretRequest.toSecret(service: Service): Secret =
	Secret(
		service = service,
		name = name,
		provider = provider,
		reference = reference,
		environment = environment,
		description = description
	)