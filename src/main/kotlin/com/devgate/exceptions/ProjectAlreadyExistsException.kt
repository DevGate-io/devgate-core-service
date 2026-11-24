package com.devgate.exceptions

import org.springframework.http.HttpStatus

class ProjectAlreadyExistsException :
	ApiException(
		message = "Project already exists",
		httpStatus = HttpStatus.CONFLICT
	)