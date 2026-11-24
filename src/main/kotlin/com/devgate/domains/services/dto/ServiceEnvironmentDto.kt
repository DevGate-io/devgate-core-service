package com.devgate.domains.services.dto

import com.devgate.domains.services.models.Service
import com.devgate.domains.services.models.ServiceEnvironment
import com.devgate.domains.services.models.enums.ServiceEnvironmentName

data class ServiceEnvironmentDto(
	val name: ServiceEnvironmentName,
	val url: String? = null
)

fun ServiceEnvironmentDto.toEnvironment(service: Service): ServiceEnvironment =
	ServiceEnvironment(
		service = service,
		name = name,
		url = url
	)

fun ServiceEnvironment.toDto(): ServiceEnvironmentDto =
	ServiceEnvironmentDto(
		name = name,
		url = url
	)