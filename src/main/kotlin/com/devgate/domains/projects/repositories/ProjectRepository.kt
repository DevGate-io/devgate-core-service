package com.devgate.domains.projects.repositories

import com.devgate.domains.projects.models.Project
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ProjectRepository : JpaRepository<Project, UUID> {
	fun existsBySlug(slug: String): Boolean
}