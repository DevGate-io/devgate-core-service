package com.devgate.domains.services.repositories

import com.devgate.domains.services.models.Service
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ServiceRepository : JpaRepository<Service, UUID> {
	fun existsBySlug(slug: String): Boolean
}