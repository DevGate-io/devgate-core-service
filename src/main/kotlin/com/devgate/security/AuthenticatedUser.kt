package com.devgate.security

import java.security.Principal
import java.util.UUID

/**
 * Authenticated caller. For platform users the id and role come from the `users` table (so a
 * role change applies immediately); for callers that have no row there — service tokens of
 * devgate-readme-service / devgate-daily-summary-service, or tokens issued before the claims
 * were introduced — they come from the access-token claims and may be null.
 *
 * Implements [Principal] so that `Authentication.getName()` returns the email, which the
 * identity code (UserService.getCurrentUser, TeamService) relies on.
 */
data class AuthenticatedUser(
	val id: UUID?,
	val email: String,
	val role: String?
) : Principal {
	override fun getName(): String = email
}