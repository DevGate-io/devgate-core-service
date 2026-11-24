package com.devgate.domains.audit.dto

import com.devgate.domains.audit.models.Action
import java.time.LocalDateTime
import java.util.UUID

/**
 * Wire contract with devgate-audit-service — keep the shape in sync with
 * com.devgate.audit.dto.LogMessagePayload there.
 */
data class LogMessagePayload(
	val action: Action? = null,
	val actorId: UUID? = null,
	val target: Target? = null,
	val payload: String? = null,
	val createdAt: LocalDateTime? = LocalDateTime.now()
)