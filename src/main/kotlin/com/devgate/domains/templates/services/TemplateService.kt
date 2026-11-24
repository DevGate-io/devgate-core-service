package com.devgate.domains.templates.services

import com.devgate.domains.templates.dto.CreateTemplateRequest
import com.devgate.domains.templates.dto.RunTemplateRequest
import com.devgate.domains.templates.dto.RunTemplateResponse
import com.devgate.domains.templates.dto.TemplateResponse
import com.devgate.domains.templates.dto.UpdateTemplateRequest
import java.util.UUID

interface TemplateService {
	fun getAllTemplates(search: String? = null): List<TemplateResponse>

	fun getTemplateById(id: UUID): TemplateResponse

	fun createTemplate(request: CreateTemplateRequest): TemplateResponse

	fun updateTemplate(
		id: UUID,
		request: UpdateTemplateRequest
	): TemplateResponse

	fun deleteTemplate(id: UUID)

	fun runTemplate(
		id: UUID,
		request: RunTemplateRequest,
		authorizationHeader: String
	): RunTemplateResponse
}