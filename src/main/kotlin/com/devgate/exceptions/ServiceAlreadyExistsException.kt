package com.devgate.exceptions

import org.springframework.http.HttpStatus

class ServiceAlreadyExistsException :
	ApiException(
		message = "Service already exists",
		httpStatus = HttpStatus.CONFLICT
	)