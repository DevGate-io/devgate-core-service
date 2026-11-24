package com.devgate.domains.templates.models

import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table
import org.hibernate.annotations.UuidGenerator
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "templates")
class Template(
	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.TIME)
	var id: UUID? = null,
	@Column(nullable = false)
	var name: String,
	@Column(nullable = false, unique = true)
	var slug: String,
	var description: String = "",
	@Column(name = "integration_id", nullable = false)
	var integrationId: UUID,
	@Column(name = "template_owner", nullable = false)
	var templateOwner: String,
	@Column(name = "template_repo", nullable = false)
	var templateRepo: String,
	@Column(nullable = false)
	var createdAt: Instant = Instant.now()
) {
	@ElementCollection
	@CollectionTable(name = "template_tags", joinColumns = [JoinColumn(name = "template_id")])
	@Column(name = "tag")
	var tags: MutableList<String> = mutableListOf()
}