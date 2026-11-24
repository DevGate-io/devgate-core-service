package com.devgate.domains.services.services.impl

import com.devgate.domains.projects.repositories.ProjectRepository
import com.devgate.domains.services.dto.CreateServiceRequest
import com.devgate.domains.services.dto.ServiceResponse
import com.devgate.domains.services.dto.UpdateServiceRequest
import com.devgate.domains.services.dto.applyPatch
import com.devgate.domains.services.dto.toResponse
import com.devgate.domains.services.dto.toService
import com.devgate.domains.services.models.Service
import com.devgate.domains.services.models.enums.ServiceHealth
import com.devgate.domains.services.repositories.ServiceRepository
import com.devgate.domains.services.services.ServiceCatalogService
import com.devgate.exceptions.ProjectNotFoundException
import com.devgate.exceptions.ServiceAlreadyExistsException
import com.devgate.exceptions.ServiceNotFoundException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import org.springframework.stereotype.Service as SpringService

// The catalog entity is named `Service` (per the DevGate domain model), which collides with
// Spring's @Service stereotype — aliased on import instead of renaming the entity.
@SpringService
class ServiceCatalogServiceImpl
	@Autowired
	constructor(
		private val serviceRepository: ServiceRepository,
		private val projectRepository: ProjectRepository
	) : ServiceCatalogService {
		@Transactional(readOnly = true)
		override fun getAllServices(
			search: String?,
			tags: List<String>?,
			health: ServiceHealth?,
			ownerTeamId: UUID?,
			projectId: UUID?
		): List<ServiceResponse> {
			val all = serviceRepository.findAll()
			val query = search?.trim()?.lowercase().orEmpty()
			val tagFilter = tags?.filter { it.isNotBlank() }.orEmpty()

			return all
				.asSequence()
				.filter { health == null || it.health == health }
				.filter { ownerTeamId == null || it.ownerTeamId == ownerTeamId }
				.filter { projectId == null || it.project.id == projectId }
				.filter { tagFilter.isEmpty() || tagFilter.all { tag -> it.tags.contains(tag) } }
				.filter {
					query.isEmpty() ||
						it.name.lowercase().contains(query) ||
						it.description.lowercase().contains(query)
				}.map { it.toResponse() }
				.toList()
		}

		@Transactional(readOnly = true)
		override fun getServiceById(id: UUID): ServiceResponse = findService(id).toResponse()

		@Transactional(readOnly = true)
		override fun getAllTags(): List<String> =
			serviceRepository
				.findAll()
				.flatMap { it.tags }
				.distinct()
				.sorted()

		@Transactional
		override fun createService(request: CreateServiceRequest): ServiceResponse {
			if (serviceRepository.existsBySlug(request.slug)) {
				throw ServiceAlreadyExistsException()
			}

			val project = projectRepository.findById(request.projectId).orElseThrow { ProjectNotFoundException() }

			return serviceRepository.save(request.toService(project)).toResponse()
		}

		@Transactional(readOnly = true)
		override fun validateServiceCreation(
			slug: String,
			projectId: UUID
		) {
			if (serviceRepository.existsBySlug(slug)) {
				throw ServiceAlreadyExistsException()
			}

			if (!projectRepository.existsById(projectId)) {
				throw ProjectNotFoundException()
			}
		}

		@Transactional
		override fun updateService(
			id: UUID,
			request: UpdateServiceRequest
		): ServiceResponse {
			val service = findService(id)

			request.slug?.let { slug ->
				if (slug != service.slug && serviceRepository.existsBySlug(slug)) {
					throw ServiceAlreadyExistsException()
				}
			}

			val project =
				request.projectId?.let {
					projectRepository.findById(it).orElseThrow { ProjectNotFoundException() }
				}

			return serviceRepository.save(service.applyPatch(request, project)).toResponse()
		}

		@Transactional
		override fun deleteService(id: UUID) {
			val service = findService(id)
			serviceRepository.delete(service)
		}

		private fun findService(id: UUID): Service = serviceRepository.findById(id).orElseThrow { ServiceNotFoundException() }
	}