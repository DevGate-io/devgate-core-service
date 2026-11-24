package com.devgate.security

import com.devgate.Constants
import com.devgate.domains.users.repositories.UserRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetails
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class RequestFilter(
	private val tokenValidator: JwtTokenValidator,
	private val userRepository: UserRepository
) : OncePerRequestFilter() {
	override fun doFilterInternal(
		request: HttpServletRequest,
		response: HttpServletResponse,
		filterChain: FilterChain
	) {
		val header = request.getHeader(Constants.AUTH_HEADER)

		if (header != null && header.startsWith(Constants.AUTH_PREFIX)) {
			val token = header.removePrefix(Constants.AUTH_PREFIX)
			val claims = tokenValidator.parseAccessToken(token)

			// A broken or expired token must answer 401 so that the frontend interceptor
			// can refresh the access token and retry the request.
			if (claims == null) {
				response.sendError(SC_UNAUTHORIZED, "Invalid or expired access token")
				return
			}

			authenticate(resolveUser(tokenValidator.toAuthenticatedUser(claims)), request)
		}

		filterChain.doFilter(request, response)
	}

	// The users table is the source of truth for platform users: a demoted admin loses the role
	// right away instead of after the access token expires. Service tokens have no row there
	// and keep the identity from their claims.
	private fun resolveUser(fromClaims: AuthenticatedUser): AuthenticatedUser {
		val user = userRepository.findByEmail(fromClaims.email) ?: return fromClaims

		return AuthenticatedUser(id = user.id, email = user.email, role = user.role.name)
	}

	private fun authenticate(
		user: AuthenticatedUser,
		request: HttpServletRequest
	) {
		val authorities = user.role?.let { listOf(SimpleGrantedAuthority("$ROLE_PREFIX$it")) }.orEmpty()
		val authentication = UsernamePasswordAuthenticationToken(user, null, authorities)

		authentication.details = WebAuthenticationDetails(request)
		SecurityContextHolder.getContext().authentication = authentication
	}

	private companion object {
		const val ROLE_PREFIX = "ROLE_"
	}
}