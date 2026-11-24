package com.devgate.exceptions

import org.springframework.http.HttpStatus

class TemplateNotFoundException :
	ApiException(
		message = "Template not found",
		httpStatus = HttpStatus.NOT_FOUND
	)