package com.devgate.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Base64
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

enum class JwtType {
	ACCESS,
	REFRESH
}

@Component
class JwtTokenValidator(
	@Value($$"${jwt.secret}")
	private val jwtSecret: String
) {
	private val logger = LoggerFactory.getLogger(this::class.java)

	private val signingKey: SecretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(jwtSecret))

	/**
	 * Returns the claims of a valid, non-expired ACCESS token, or null when the token
	 * is malformed, wrongly signed, expired or is not an access token.
	 */
	fun parseAccessToken(token: String): Claims? {
		val claims =
			try {
				Jwts
					.parser()
					.verifyWith(signingKey)
					.build()
					.parseSignedClaims(token)
					.payload
			} catch (exception: JwtException) {
				logger.debug("JwtTokenValidator[parseAccessToken]: rejected token -> ${exception.message}")
				return null
			} catch (exception: IllegalArgumentException) {
				logger.debug("JwtTokenValidator[parseAccessToken]: empty token -> ${exception.message}")
				return null
			}

		if (claims["type"] != JwtType.ACCESS.name) {
			logger.debug("JwtTokenValidator[parseAccessToken]: not an access token")
			return null
		}

		val expiration = claims.expiration

		if (expiration == null || !expiration.after(Date())) {
			logger.debug("JwtTokenValidator[parseAccessToken]: expired token")
			return null
		}

		return claims
	}

	fun toAuthenticatedUser(claims: Claims): AuthenticatedUser =
		AuthenticatedUser(
			id = claims["userId"]?.toString()?.let { runCatching { UUID.fromString(it) }.getOrNull() },
			email = claims.subject,
			role = claims["role"]?.toString()
		)
}