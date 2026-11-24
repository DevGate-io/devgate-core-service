package com.devgate.domains.templates.services.impl

import com.devgate.domains.services.dto.CreateServiceRequest
import com.devgate.domains.services.services.ServiceCatalogService
import com.devgate.domains.templates.clients.IntegrationServiceClient
import com.devgate.domains.templates.clients.dto.GenerateRepoRequest
import com.devgate.domains.templates.dto.CreateTemplateRequest
import com.devgate.domains.templates.dto.RunTemplateRequest
import com.devgate.domains.templates.dto.RunTemplateResponse
import com.devgate.domains.templates.dto.TemplateResponse
import com.devgate.domains.templates.dto.UpdateTemplateRequest
import com.devgate.domains.templates.dto.toResponse
import com.devgate.domains.templates.models.Template
import com.devgate.domains.templates.repositories.TemplateRepository
import com.devgate.domains.templates.services.TemplateService
import com.devgate.exceptions.TemplateAlreadyExistsException
import com.devgate.exceptions.TemplateNotFoundException
import com.devgate.utils.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import java.util.UUID

@Service
class TemplateServiceImpl
	@Autowired
	constructor(
		private val templateRepository: TemplateRepository,
		private val integrationServiceClient: IntegrationServiceClient,
		private val serviceCatalogService: ServiceCatalogService,
		transactionManager: PlatformTransactionManager
	) : TemplateService {
		private val readOnlyTransaction = TransactionTemplate(transactionManager).apply { isReadOnly = true }

		@Transactional(readOnly = true)
		override fun getAllTemplates(search: String?): List<TemplateResponse> {
			val all = templateRepository.findAll()
			val query = search?.trim()?.lowercase().orEmpty()

			return all
				.asSequence()
				.filter {
					query.isEmpty() ||
						it.name.lowercase().contains(query) ||
						it.slug.lowercase().contains(query) ||
						it.description.lowercase().contains(query)
				}.map { it.toResponse() }
				.toList()
		}

		@Transactional(readOnly = true)
		override fun getTemplateById(id: UUID): TemplateResponse = findTemplate(id).toResponse()

		@Transactional
		override fun createTemplate(request: CreateTemplateRequest): TemplateResponse {
			if (templateRepository.existsBySlug(request.slug)) {
				throw TemplateAlreadyExistsException()
			}

			val template =
				Template(
					name = request.name,
					slug = request.slug,
					description = request.description ?: "",
					integrationId = request.integrationId,
					templateOwner = request.templateOwner,
					templateRepo = request.templateRepo
				)
			template.tags = request.tags.toMutableList()

			return templateRepository.save(template).toResponse()
		}

		@Transactional
		override fun updateTemplate(
			id: UUID,
			request: UpdateTemplateRequest
		): TemplateResponse {
			val template = findTemplate(id)

			request.slug?.let { slug ->
				if (slug != template.slug && templateRepository.existsBySlug(slug)) {
					throw TemplateAlreadyExistsException()
				}
				template.slug = slug
			}
			request.name?.let { template.name = it }
			request.description?.let { template.description = it }
			request.tags?.let { template.tags = it.toMutableList() }
			request.integrationId?.let { template.integrationId = it }
			request.templateOwner?.let { template.templateOwner = it }
			request.templateRepo?.let { template.templateRepo = it }

			return templateRepository.save(template).toResponse()
		}

		@Transactional
		override fun deleteTemplate(id: UUID) {
			templateRepository.delete(findTemplate(id))
		}

		// Deliberately not @Transactional: the run calls devgate-integration-service over HTTP, and
		// a DB transaction must not be held open across that call. The template is read in its own
		// short read-only transaction (its tags are a lazy collection), the Service is created in
		// ServiceCatalogService's own transaction.
		override fun runTemplate(
			id: UUID,
			request: RunTemplateRequest,
			authorizationHeader: String
		): RunTemplateResponse {
			val template = requireNotNull(readOnlyTransaction.execute { findTemplate(id).toResponse() })

			// Reject what the catalog would reject before the GitHub repository exists, so the
			// common failures (duplicate slug, unknown project) do not leave an orphaned repository.
			serviceCatalogService.validateServiceCreation(slug = request.newRepoName, projectId = request.projectId)

			val repository =
				integrationServiceClient.generateRepoFromTemplate(
					integrationId = template.integrationId,
					request =
						GenerateRepoRequest(
							templateOwner = template.templateOwner,
							templateRepo = template.templateRepo,
							newRepoName = request.newRepoName,
							private = request.private,
							description = request.description
						),
					authorizationHeader = authorizationHeader
				)

			val service =
				try {
					serviceCatalogService.createService(
						CreateServiceRequest(
							name = request.newRepoName,
							slug = request.newRepoName,
							ownerTeamId = request.ownerTeamId,
							projectId = request.projectId,
							repoUrl = repository.htmlUrl,
							tags = template.tags
						)
					)
				} catch (exception: RuntimeException) {
					// No compensating transaction: the GitHub repo already exists at this point
					// (e.g. a concurrent run took the slug after validation) — log the orphaned
					// repo URL for manual cleanup, then let the caller see the failure.
					Logger.error(
						"TemplateServiceImpl[runTemplate]: Service creation failed after GitHub repo " +
							"${repository.htmlUrl} was already created from template ${template.slug} — " +
							"manual cleanup required. ${exception.message}",
						this
					)
					throw exception
				}

			return RunTemplateResponse(service = service)
		}

		private fun findTemplate(id: UUID): Template =
			templateRepository.findById(id).orElseThrow { TemplateNotFoundException() }
	}