package com.devgate.domains.services.models

import com.devgate.domains.projects.models.Project
import com.devgate.domains.services.models.enums.ServiceHealth
import jakarta.persistence.CascadeType
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.annotations.UuidGenerator
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "services")
class Service(
	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.TIME)
	var id: UUID? = null,
	@ManyToOne
	@JoinColumn(name = "project_id", nullable = false)
	var project: Project,
	@Column(name = "owner_team_id", nullable = false)
	var ownerTeamId: UUID,
	@Column(nullable = false)
	var name: String,
	@Column(nullable = false, unique = true)
	var slug: String,
	var description: String = "",
	var language: String? = null,
	@Column(name = "repo_url")
	var repoUrl: String? = null,
	@Column(name = "pipeline_url")
	var pipelineUrl: String? = null,
	@Column(name = "docs_url")
	var docsUrl: String? = null,
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	var health: ServiceHealth = ServiceHealth.UNKNOWN,
	@Column(name = "slo_availability")
	var sloAvailability: Double? = null,
	@Column(name = "slo_latency_p95_ms")
	var sloLatencyP95Ms: Int? = null,
	@Column(nullable = false)
	var createdAt: Instant = Instant.now(),
	@Column(nullable = false)
	var updatedAt: Instant = Instant.now()
) {
	@ElementCollection
	@CollectionTable(name = "service_tags", joinColumns = [JoinColumn(name = "service_id")])
	@Column(name = "tag")
	var tags: MutableList<String> = mutableListOf()

	@ElementCollection
	@CollectionTable(name = "service_dependencies", joinColumns = [JoinColumn(name = "service_id")])
	@Column(name = "dependency_service_id")
	var dependencies: MutableList<String> = mutableListOf()

	@OneToMany(mappedBy = "service", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
	var environments: MutableList<ServiceEnvironment> = mutableListOf()
}