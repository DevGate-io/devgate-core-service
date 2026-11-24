package com.devgate.domains.audit.services

import com.devgate.domains.audit.dto.Target
import com.devgate.domains.audit.models.Action

interface AuditLogService {
	fun sendMessage(
		action: Action,
		target: Target
	)
}