package com.devgate.domains.secrets.services

import com.devgate.domains.secrets.dto.CreateSecretRequest
import com.devgate.domains.secrets.dto.SecretResponse
import com.devgate.domains.secrets.dto.UpdateSecretRequest
import java.util.UUID

interface SecretService {
	fun getAllForService(serviceId: UUID): List<SecretResponse>

	fun getSecret(
		serviceId: UUID,
		id: UUID
	): SecretResponse

	fun createSecret(
		serviceId: UUID,
		request: CreateSecretRequest
	): SecretResponse

	fun updateSecret(
		serviceId: UUID,
		id: UUID,
		request: UpdateSecretRequest
	): SecretResponse

	fun deleteSecret(
		serviceId: UUID,
		id: UUID
	)

	fun rotateSecret(
		serviceId: UUID,
		id: UUID
	): SecretResponse
}