package com.devgate.security

import com.devgate.domains.users.models.User
import com.devgate.domains.users.models.enums.Role
import com.devgate.domains.users.repositories.UserRepository
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import java.time.Instant
import java.util.Base64
import java.util.Date
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RequestFilterTest {
	private val secret = "ODVkYmUxNWQ3NWVmOTMwOGM3YWUwZjMzYzdhMzI0Y2M2ZjRiZjUxOWEyZWQyZjMwMjdiZDMzYzE0MGE0ZjlhYQ=="
	private val userRepository = Mockito.mock(UserRepository::class.java)
	private val filter = RequestFilter(JwtTokenValidator(secret), userRepository)

	@AfterEach
	fun clearContext() {
		SecurityContextHolder.clearContext()
	}

	private fun accessToken(
		email: String,
		userId: UUID,
		role: String,
		expiresAt: Instant = Instant.now().plusSeconds(600)
	): String =
		Jwts
			.builder()
			.subject(email)
			.claim("type", JwtType.ACCESS.name)
			.claim("userId", userId.toString())
			.claim("role", role)
			.issuedAt(Date())
			.expiration(Date.from(expiresAt))
			.signWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret)), Jwts.SIG.HS256)
			.compact()

	private fun requestWith(token: String) = MockHttpServletRequest().apply { addHeader("Authorization", "Bearer $token") }

	@Test
	fun `role and id come from the users table when the token subject is a platform user`() {
		val dbId = UUID.randomUUID()
		val user =
			User(
				id = dbId,
				fullName = "Demoted Admin",
				role = Role.MEMBER,
				email = "user@devgate.local",
				hashedPassword = "hash"
			)
		Mockito.`when`(userRepository.findByEmail("user@devgate.local")).thenReturn(user)
		val chain = MockFilterChain()

		val token = accessToken("user@devgate.local", UUID.randomUUID(), "ADMIN")

		filter.doFilter(requestWith(token), MockHttpServletResponse(), chain)

		val authentication = assertNotNull(SecurityContextHolder.getContext().authentication)
		val principal = authentication.principal as AuthenticatedUser
		assertEquals(dbId, principal.id)
		assertEquals("user@devgate.local", authentication.name)
		assertEquals(listOf("ROLE_MEMBER"), authentication.authorities.map { it.authority })
		assertNotNull(chain.request)
	}

	@Test
	fun `service tokens without a users row keep the identity from their claims`() {
		val systemId = UUID.fromString("00000000-0000-0000-0000-000000000000")
		Mockito.`when`(userRepository.findByEmail("readme-worker@devgate.local")).thenReturn(null)

		val token = accessToken("readme-worker@devgate.local", systemId, "ADMIN")

		filter.doFilter(requestWith(token), MockHttpServletResponse(), MockFilterChain())

		val authentication = assertNotNull(SecurityContextHolder.getContext().authentication)
		assertEquals(systemId, (authentication.principal as AuthenticatedUser).id)
		assertEquals(listOf("ROLE_ADMIN"), authentication.authorities.map { it.authority })
	}

	@Test
	fun `an expired token answers 401 and stops the chain`() {
		val response = MockHttpServletResponse()
		val chain = MockFilterChain()
		val expired =
			accessToken("user@devgate.local", UUID.randomUUID(), "ADMIN", Instant.now().minusSeconds(60))

		filter.doFilter(requestWith(expired), response, chain)

		assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.status)
		assertNull(chain.request)
		assertNull(SecurityContextHolder.getContext().authentication)
	}

	@Test
	fun `requests without a bearer token pass through unauthenticated`() {
		val chain = MockFilterChain()

		filter.doFilter(MockHttpServletRequest(), MockHttpServletResponse(), chain)

		assertNotNull(chain.request)
		assertNull(SecurityContextHolder.getContext().authentication)
		Mockito.verifyNoInteractions(userRepository)
	}
}