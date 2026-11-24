package com.devgate.exceptions

import org.springframework.http.HttpStatus

class ServiceNotFoundException :
	ApiException(
		message = "Service not found",
		httpStatus = HttpStatus.NOT_FOUND
	)