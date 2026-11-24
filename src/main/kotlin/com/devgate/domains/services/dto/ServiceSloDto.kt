package com.devgate.domains.services.dto

data class ServiceSloDto(
	val availability: Double,
	val latencyP95Ms: Int? = null
)