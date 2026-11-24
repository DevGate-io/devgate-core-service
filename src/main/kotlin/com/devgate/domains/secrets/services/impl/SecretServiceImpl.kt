package com.devgate.domains.secrets.services.impl

import com.devgate.domains.secrets.dto.CreateSecretRequest
import com.devgate.domains.secrets.dto.SecretResponse
import com.devgate.domains.secrets.dto.UpdateSecretRequest
import com.devgate.domains.secrets.dto.applyPatch
import com.devgate.domains.secrets.dto.toResponse
import com.devgate.domains.secrets.dto.toSecret
import com.devgate.domains.secrets.models.Secret
import com.devgate.domains.secrets.repositories.SecretRepository
import com.devgate.domains.secrets.services.SecretService
import com.devgate.domains.services.repositories.ServiceRepository
import com.devgate.exceptions.SecretNotFoundException
import com.devgate.exceptions.ServiceNotFoundException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class SecretServiceImpl
	@Autowired
	constructor(
		private val secretRepository: SecretRepository,
		private val serviceRepository: ServiceRepository
	) : SecretService {
		@Transactional(readOnly = true)
		override fun getAllForService(serviceId: UUID): List<SecretResponse> {
			requireService(serviceId)

			return secretRepository.findByServiceId(serviceId).map { it.toResponse() }
		}

		@Transactional(readOnly = true)
		override fun getSecret(
			serviceId: UUID,
			id: UUID
		): SecretResponse = findSecret(serviceId, id).toResponse()

		@Transactional
		override fun createSecret(
			serviceId: UUID,
			request: CreateSecretRequest
		): SecretResponse {
			val service = requireService(serviceId)

			return secretRepository.save(request.toSecret(service)).toResponse()
		}

		@Transactional
		override fun updateSecret(
			serviceId: UUID,
			id: UUID,
			request: UpdateSecretRequest
		): SecretResponse {
			val secret = findSecret(serviceId, id)

			return secretRepository.save(secret.applyPatch(request)).toResponse()
		}

		@Transactional
		override fun deleteSecret(
			serviceId: UUID,
			id: UUID
		) {
			secretRepository.delete(findSecret(serviceId, id))
		}

		@Transactional
		override fun rotateSecret(
			serviceId: UUID,
			id: UUID
		): SecretResponse {
			val secret = findSecret(serviceId, id)
			secret.lastRotatedAt = Instant.now()

			return secretRepository.save(secret).toResponse()
		}

		private fun requireService(serviceId: UUID) =
			serviceRepository.findById(serviceId).orElseThrow { ServiceNotFoundException() }

		private fun findSecret(
			serviceId: UUID,
			id: UUID
		): Secret {
			val secret = secretRepository.findById(id).orElseThrow { SecretNotFoundException() }

			// A secret belonging to a different service must look identical to a missing secret
			// under this nested resource path, not leak that the id exists elsewhere.
			if (secret.service.id != serviceId) {
				throw SecretNotFoundException()
			}

			return secret
		}
	}