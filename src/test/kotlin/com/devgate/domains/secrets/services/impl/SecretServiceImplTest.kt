package com.devgate.domains.secrets.services.impl

import com.devgate.domains.projects.models.Project
import com.devgate.domains.secrets.dto.CreateSecretRequest
import com.devgate.domains.secrets.models.Secret
import com.devgate.domains.secrets.models.enums.SecretProvider
import com.devgate.domains.secrets.repositories.SecretRepository
import com.devgate.domains.services.models.Service
import com.devgate.domains.services.models.enums.ServiceHealth
import com.devgate.domains.services.repositories.ServiceRepository
import com.devgate.exceptions.SecretNotFoundException
import com.devgate.exceptions.ServiceNotFoundException
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class SecretServiceImplTest {
	private fun project() =
		Project(teamId = UUID.randomUUID(), name = "Platform", slug = "platform").apply {
			id = UUID.randomUUID()
		}

	private fun service() =
		Service(
			project = project(),
			ownerTeamId = UUID.randomUUID(),
			name = "catalog-service",
			slug = "catalog-service",
			health = ServiceHealth.HEALTHY
		).apply { id = UUID.randomUUID() }

	private fun secret(owner: Service) =
		Secret(
			service = owner,
			name = "db-password",
			provider = SecretProvider.VAULT,
			reference = "secret/data/catalog/db-password"
		).apply { id = UUID.randomUUID() }

	@Test
	fun `createSecret throws when the parent service does not exist`() {
		val secretRepository = Mockito.mock(SecretRepository::class.java)
		val serviceRepository = Mockito.mock(ServiceRepository::class.java)
		val serviceId = UUID.randomUUID()
		Mockito.`when`(serviceRepository.findById(serviceId)).thenReturn(Optional.empty())

		val secretService = SecretServiceImpl(secretRepository, serviceRepository)
		val request = CreateSecretRequest(name = "db-password", provider = SecretProvider.VAULT, reference = "secret/data/x")

		assertFailsWith<ServiceNotFoundException> { secretService.createSecret(serviceId, request) }
	}

	@Test
	fun `getAllForService throws when the parent service does not exist`() {
		val secretRepository = Mockito.mock(SecretRepository::class.java)
		val serviceRepository = Mockito.mock(ServiceRepository::class.java)
		val serviceId = UUID.randomUUID()
		Mockito.`when`(serviceRepository.findById(serviceId)).thenReturn(Optional.empty())

		val secretService = SecretServiceImpl(secretRepository, serviceRepository)

		assertFailsWith<ServiceNotFoundException> { secretService.getAllForService(serviceId) }
	}

	@Test
	fun `getSecret throws when the secret belongs to a different service`() {
		val secretRepository = Mockito.mock(SecretRepository::class.java)
		val serviceRepository = Mockito.mock(ServiceRepository::class.java)
		val owningService = service()
		val secret = secret(owningService)
		Mockito.`when`(secretRepository.findById(requireNotNull(secret.id))).thenReturn(Optional.of(secret))

		val secretService = SecretServiceImpl(secretRepository, serviceRepository)
		val otherServiceId = UUID.randomUUID()

		assertFailsWith<SecretNotFoundException> {
			secretService.getSecret(otherServiceId, requireNotNull(secret.id))
		}
	}

	@Test
	fun `rotateSecret sets lastRotatedAt and persists the change`() {
		val secretRepository = Mockito.mock(SecretRepository::class.java)
		val serviceRepository = Mockito.mock(ServiceRepository::class.java)
		val owningService = service()
		val secret = secret(owningService)
		Mockito.`when`(secretRepository.findById(requireNotNull(secret.id))).thenReturn(Optional.of(secret))
		Mockito.`when`(secretRepository.save(secret)).thenReturn(secret)

		val secretService = SecretServiceImpl(secretRepository, serviceRepository)
		val response = secretService.rotateSecret(requireNotNull(owningService.id), requireNotNull(secret.id))

		assertNotNull(response.lastRotatedAt)
		assertEquals(secret.lastRotatedAt, response.lastRotatedAt)
		Mockito.verify(secretRepository).save(secret)
	}
}