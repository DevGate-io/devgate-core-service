package com.devgate.domains.templates.dto

import com.devgate.domains.audit.dto.Target
import com.devgate.domains.templates.models.Template
import java.time.Instant
import java.util.UUID

data class TemplateResponse(
	val id: UUID?,
	val name: String,
	val slug: String,
	val description: String,
	val tags: List<String>,
	val integrationId: UUID,
	val templateOwner: String,
	val templateRepo: String,
	val createdAt: Instant
)

fun TemplateResponse.toTarget(): Target =
	Target(
		type = "template",
		id = id.toString(),
		label = name
	)

fun Template.toResponse(): TemplateResponse =
	TemplateResponse(
		id = id,
		name = name,
		slug = slug,
		description = description,
		tags = tags.toList(),
		integrationId = integrationId,
		templateOwner = templateOwner,
		templateRepo = templateRepo,
		createdAt = createdAt
	)