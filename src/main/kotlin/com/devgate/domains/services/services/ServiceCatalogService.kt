package com.devgate.domains.services.services

import com.devgate.domains.services.dto.CreateServiceRequest
import com.devgate.domains.services.dto.ServiceResponse
import com.devgate.domains.services.dto.UpdateServiceRequest
import com.devgate.domains.services.models.enums.ServiceHealth
import java.util.UUID

interface ServiceCatalogService {
	fun getAllServices(
		search: String? = null,
		tags: List<String>? = null,
		health: ServiceHealth? = null,
		ownerTeamId: UUID? = null,
		projectId: UUID? = null
	): List<ServiceResponse>

	fun getServiceById(id: UUID): ServiceResponse

	fun getAllTags(): List<String>

	fun createService(request: CreateServiceRequest): ServiceResponse

	/**
	 * Fails with the same errors [createService] would (duplicate slug, unknown project) without
	 * writing anything — lets the scaffolder reject a run before it creates an external repository.
	 */
	fun validateServiceCreation(
		slug: String,
		projectId: UUID
	)

	fun updateService(
		id: UUID,
		request: UpdateServiceRequest
	): ServiceResponse

	fun deleteService(id: UUID)
}