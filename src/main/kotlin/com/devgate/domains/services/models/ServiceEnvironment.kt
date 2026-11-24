package com.devgate.domains.services.models

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
import java.util.UUID

@Entity
@Table(name = "service_environments")
class ServiceEnvironment(
	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.TIME)
	var id: UUID? = null,
	@ManyToOne
	@JoinColumn(name = "service_id", nullable = false)
	var service: Service,
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	var name: ServiceEnvironmentName,
	var url: String? = null
)