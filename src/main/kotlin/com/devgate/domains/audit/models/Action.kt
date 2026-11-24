package com.devgate.domains.audit.models

import com.fasterxml.jackson.annotation.JsonValue

/**
 * Every action published by this service. devgate-audit-service deserializes `action` strictly
 * into its own enum (com.devgate.audit.models.Action) — any new value here must be mirrored there
 * and deployed together with this service, or the first such event breaks deserialization.
 */
enum class Action(
	val value: String
) {
	USER_CREATED("user.created"),
	USER_UPDATED("user.updated"),
	USER_DELETED("user.deleted"),
	ROLE_CHANGED("user.role_changed"),

	TEAM_CREATED("team.created"),
	TEAM_UPDATED("team.updated"),
	TEAM_DELETED("team.deleted"),
	TEAM_MEMBER_ADDED("team.member.added"),
	TEAM_MEMBER_REMOVED("team.member.removed"),
	TEAM_MEMBER_UPDATED("team.member.updated"),

	ORGANIZATION_CREATED("organization.created"),
	DEPARTMENT_CREATED("department.created"),
	DEPARTMENT_UPDATED("department.updated"),
	DEPARTMENT_DELETED("department.deleted"),

	PROJECT_CREATED("project.created"),
	PROJECT_UPDATED("project.updated"),
	PROJECT_DELETED("project.deleted"),

	SERVICE_CREATED("service.created"),
	SERVICE_UPDATED("service.updated"),
	SERVICE_DELETED("service.deleted"),

	SECRET_CREATED("secret.created"),
	SECRET_UPDATED("secret.updated"),
	SECRET_DELETED("secret.deleted"),
	SECRET_ROTATED("secret.rotated"),

	TEMPLATE_USED("template.used");

	@JsonValue
	fun toValue() = value
}