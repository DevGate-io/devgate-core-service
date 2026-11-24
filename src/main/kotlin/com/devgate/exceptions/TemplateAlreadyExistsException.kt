package com.devgate.exceptions

import org.springframework.http.HttpStatus

class TemplateAlreadyExistsException :
	ApiException(
		message = "Template already exists",
		httpStatus = HttpStatus.CONFLICT
	)