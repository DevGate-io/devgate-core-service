package com.devgate.domains.secrets.repositories

import com.devgate.domains.secrets.models.Secret
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SecretRepository : JpaRepository<Secret, UUID> {
	fun findByServiceId(serviceId: UUID): List<Secret>
}