package com.devgate.domains.secrets.models.enums

import com.fasterxml.jackson.annotation.JsonValue

enum class SecretProvider(
	val value: String
) {
	VAULT("vault"),
	AWS_SECRETS_MANAGER("aws_secrets_manager"),
	AZURE_KEY_VAULT("azure_key_vault"),
	GCP_SECRET_MANAGER("gcp_secret_manager"),
	OTHER("other");

	@JsonValue
	fun toValue() = value
}