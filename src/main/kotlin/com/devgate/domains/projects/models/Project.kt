package com.devgate.domains.projects.models

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.UuidGenerator
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "projects")
class Project(
	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.TIME)
	var id: UUID? = null,
	@Column(name = "team_id", nullable = false)
	var teamId: UUID,
	@Column(nullable = false)
	var name: String,
	var description: String = "",
	@Column(nullable = false, unique = true)
	var slug: String,
	@Column(nullable = false)
	var createdAt: Instant = Instant.now(),
	@Column(nullable = false)
	var updatedAt: Instant = Instant.now()
)