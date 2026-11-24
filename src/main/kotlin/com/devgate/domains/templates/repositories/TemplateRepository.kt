package com.devgate.domains.templates.repositories

import com.devgate.domains.templates.models.Template
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface TemplateRepository : JpaRepository<Template, UUID> {
	fun existsBySlug(slug: String): Boolean
}