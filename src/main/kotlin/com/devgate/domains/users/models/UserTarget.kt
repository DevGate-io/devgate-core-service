package com.devgate.domains.users.models

import com.devgate.domains.audit.dto.Target

fun User.toTarget(): Target =
	Target(
		type = "user",
		id = id.toString(),
		label = fullName
	)