package com.devgate.domains.services.models.enums

import com.fasterxml.jackson.annotation.JsonValue

enum class ServiceHealth(
	val value: String
) {
	HEALTHY("healthy"),
	DEGRADED("degraded"),
	DOWN("down"),
	UNKNOWN("unknown");

	@JsonValue
	fun toValue() = value
}