package com.devgate.domains.services.models.enums

import com.fasterxml.jackson.annotation.JsonValue

enum class ServiceEnvironmentName(
	val value: String
) {
	DEV("dev"),
	STAGE("stage"),
	PROD("prod");

	@JsonValue
	fun toValue() = value
}