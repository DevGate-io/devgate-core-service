package com.devgate.domains.templates.dto

import com.devgate.domains.audit.dto.Target
import com.devgate.domains.services.dto.ServiceResponse
import com.devgate.domains.services.dto.toTarget

data class RunTemplateResponse(
	val service: ServiceResponse
)

fun RunTemplateResponse.toTarget(): Target = service.toTarget()