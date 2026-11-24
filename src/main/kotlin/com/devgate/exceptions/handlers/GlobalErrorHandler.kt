package com.devgate.exceptions.handlers

import com.devgate.exceptions.ApiException
import com.devgate.utils.ApiError
import com.devgate.utils.ApiErrorParser
import io.jsonwebtoken.JwtException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.server.ResponseStatusException

@RestControllerAdvice
class GlobalErrorHandler {
	@ExceptionHandler(AuthenticationException::class)
	fun handleAuthenticationException(exception: AuthenticationException): ProblemDetail =
		ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, exception.message)

	@ExceptionHandler(JwtException::class)
	fun handleJwtException(exception: JwtException): ProblemDetail =
		ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, exception.message)

	// Method security (@PreAuthorize) must answer 403: the frontend interceptor treats 401
	// as "refresh the access token and retry", and a missing role is not fixable by a refresh.
	@ExceptionHandler(AccessDeniedException::class)
	fun handleAccessDeniedException(exception: AccessDeniedException): ProblemDetail =
		ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, exception.message)

	@ExceptionHandler(ApiException::class)
	fun handleApiException(exception: ApiException): ProblemDetail =
		ProblemDetail.forStatusAndDetail(exception.httpStatus, exception.message)

	@ExceptionHandler(ResponseStatusException::class)
	fun handleResponseStatusException(exception: ResponseStatusException): ProblemDetail =
		ProblemDetail.forStatusAndDetail(exception.statusCode, exception.reason)

	@ExceptionHandler(HttpClientErrorException::class)
	fun handleHttpClientErrorException(exception: HttpClientErrorException): ProblemDetail {
		val apiError: ApiError? = ApiErrorParser.parse(exception.responseBodyAsString)

		return ProblemDetail.forStatusAndDetail(exception.statusCode, apiError?.detail ?: exception.message)
	}

	@ExceptionHandler(HttpMessageNotReadableException::class)
	fun handleHttpMessageNotReadableException(exception: HttpMessageNotReadableException): ProblemDetail =
		ProblemDetail.forStatusAndDetail(
			HttpStatus.UNPROCESSABLE_ENTITY,
			exception.message
		)
}