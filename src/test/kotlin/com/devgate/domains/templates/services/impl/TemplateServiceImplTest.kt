package com.devgate.domains.templates.services.impl

import com.devgate.domains.services.dto.CreateServiceRequest
import com.devgate.domains.services.dto.ServiceResponse
import com.devgate.domains.services.models.enums.ServiceHealth
import com.devgate.domains.services.services.ServiceCatalogService
import com.devgate.domains.templates.clients.IntegrationServiceClient
import com.devgate.domains.templates.clients.dto.GenerateRepoRequest
import com.devgate.domains.templates.clients.dto.GithubRepositoryDto
import com.devgate.domains.templates.dto.RunTemplateRequest
import com.devgate.domains.templates.models.Template
import com.devgate.domains.templates.repositories.TemplateRepository
import com.devgate.exceptions.ServiceAlreadyExistsException
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.SimpleTransactionStatus
import java.time.Instant
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TemplateServiceImplTest {
	private val templateRepository = Mockito.mock(TemplateRepository::class.java)
	private val integrationClient = Mockito.mock(IntegrationServiceClient::class.java)
	private val serviceCatalogService = Mockito.mock(ServiceCatalogService::class.java)
	private val transactionManager =
		Mockito.mock(PlatformTransactionManager::class.java).also {
			Mockito.`when`(it.getTransaction(Mockito.any())).thenReturn(SimpleTransactionStatus())
		}
	private val templateService =
		TemplateServiceImpl(templateRepository, integrationClient, serviceCatalogService, transactionManager)

	private val authorization = "Bearer token"
	private val projectId = UUID.randomUUID()
	private val ownerTeamId = UUID.randomUUID()

	private val template =
		Template(
			id = UUID.randomUUID(),
			name = "Kotlin service",
			slug = "kotlin-service",
			integrationId = UUID.randomUUID(),
			templateOwner = "devgate",
			templateRepo = "kotlin-template"
		).apply { tags = mutableListOf("kotlin", "backend") }

	private val request =
		RunTemplateRequest(newRepoName = "billing", ownerTeamId = ownerTeamId, projectId = projectId)

	private val generateRequest =
		GenerateRepoRequest(
			templateOwner = "devgate",
			templateRepo = "kotlin-template",
			newRepoName = "billing",
			private = true,
			description = null
		)

	@Test
	fun `a run the catalog would reject fails before the repository is generated`() {
		Mockito.`when`(templateRepository.findById(requireNotNull(template.id))).thenReturn(Optional.of(template))
		Mockito
			.doThrow(ServiceAlreadyExistsException())
			.`when`(serviceCatalogService)
			.validateServiceCreation("billing", projectId)

		assertFailsWith<ServiceAlreadyExistsException> {
			templateService.runTemplate(requireNotNull(template.id), request, authorization)
		}

		Mockito.verifyNoInteractions(integrationClient)
	}

	@Test
	fun `a run creates the catalog Service in-process from the generated repository`() {
		val repository =
			GithubRepositoryDto(
				name = "billing",
				fullName = "devgate/billing",
				private = true,
				htmlUrl = "https://github.com/devgate/billing",
				description = null,
				defaultBranch = "main",
				updatedAt = null,
				isTemplate = false
			)
		val expectedCreate =
			CreateServiceRequest(
				name = "billing",
				slug = "billing",
				ownerTeamId = ownerTeamId,
				projectId = projectId,
				repoUrl = "https://github.com/devgate/billing",
				tags = listOf("kotlin", "backend")
			)
		val created =
			ServiceResponse(
				id = UUID.randomUUID(),
				projectId = projectId,
				ownerTeamId = ownerTeamId,
				name = "billing",
				slug = "billing",
				description = "",
				tags = listOf("kotlin", "backend"),
				language = null,
				repoUrl = "https://github.com/devgate/billing",
				pipelineUrl = null,
				docsUrl = null,
				environments = emptyList(),
				slo = null,
				health = ServiceHealth.UNKNOWN,
				dependencies = emptyList(),
				createdAt = Instant.now(),
				updatedAt = Instant.now()
			)
		Mockito.`when`(templateRepository.findById(requireNotNull(template.id))).thenReturn(Optional.of(template))
		Mockito
			.`when`(integrationClient.generateRepoFromTemplate(template.integrationId, generateRequest, authorization))
			.thenReturn(repository)
		Mockito.`when`(serviceCatalogService.createService(expectedCreate)).thenReturn(created)

		val response = templateService.runTemplate(requireNotNull(template.id), request, authorization)

		assertEquals(created, response.service)
		Mockito.verify(serviceCatalogService).validateServiceCreation("billing", projectId)
		Mockito.verify(serviceCatalogService).createService(expectedCreate)
	}
}