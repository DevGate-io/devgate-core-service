package com.devgate.exceptions

import org.springframework.http.HttpStatus

class ProjectNotFoundException :
	ApiException(
		message = "Project not found",
		httpStatus = HttpStatus.NOT_FOUND
	)