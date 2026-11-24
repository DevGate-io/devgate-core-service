package com.devgate.domains.templates.clients

import com.devgate.Constants
import com.devgate.domains.templates.clients.dto.GenerateRepoRequest
import com.devgate.domains.templates.clients.dto.GithubRepositoryDto
import com.devgate.exceptions.ApiException
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.toEntity
import java.util.UUID

// Calls the sibling devgate-integration-service to generate a new GitHub repository from a
// template repo — this service's own analog of GithubConnector's error-handling convention.
@Component
class IntegrationServiceClient(
	restClientBuilder: RestClient.Builder,
	@Value($$"${scaffolder.integration-service-url}")
	baseUrl: String
) {
	private val restClient = restClientBuilder.baseUrl(baseUrl).build()

	fun generateRepoFromTemplate(
		integrationId: UUID,
		request: GenerateRepoRequest,
		authorizationHeader: String
	): GithubRepositoryDto =
		try {
			val response =
				restClient
					.post()
					.uri("/integrations/$integrationId/generate-repo")
					.header(Constants.AUTH_HEADER, authorizationHeader)
					.body(request)
					.retrieve()
					.toEntity<GithubRepositoryDto>()

			response.body
				?: throw ApiException("Integration service returned an empty response", HttpStatus.BAD_GATEWAY)
		} catch (exception: RestClientResponseException) {
			throw ApiException(
				"Integration service rejected the repo generation request (HTTP ${exception.statusCode.value()})",
				HttpStatus.BAD_GATEWAY
			)
		} catch (exception: ApiException) {
			throw exception
		} catch (exception: Exception) {
			throw ApiException("Failed to reach integration service: ${exception.message}", HttpStatus.BAD_GATEWAY)
		}
}