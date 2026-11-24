package com.devgate.exceptions

import org.springframework.http.HttpStatus

class SecretNotFoundException :
	ApiException(
		message = "Secret not found",
		httpStatus = HttpStatus.NOT_FOUND
	)