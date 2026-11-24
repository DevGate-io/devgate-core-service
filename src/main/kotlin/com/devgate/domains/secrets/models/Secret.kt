package com.devgate.domains.secrets.models

import com.devgate.domains.secrets.models.enums.SecretProvider
import com.devgate.domains.services.models.Service
import com.devgate.domains.services.models.enums.ServiceEnvironmentName
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.UuidGenerator
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "secrets")
class Secret(
	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.TIME)
	var id: UUID? = null,
	@ManyToOne
	@JoinColumn(name = "service_id", nullable = false)
	var service: Service,
	@Column(nullable = false)
	var name: String,
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	var provider: SecretProvider,
	@Column(nullable = false)
	var reference: String,
	@Enumerated(EnumType.STRING)
	var environment: ServiceEnvironmentName? = null,
	var description: String? = null,
	@Column(name = "last_rotated_at")
	var lastRotatedAt: Instant? = null,
	@Column(nullable = false)
	var createdAt: Instant = Instant.now(),
	@Column(nullable = false)
	var updatedAt: Instant = Instant.now()
)