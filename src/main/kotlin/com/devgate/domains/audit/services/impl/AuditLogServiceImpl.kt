package com.devgate.domains.audit.services.impl

import com.devgate.domains.audit.dto.LogMessagePayload
import com.devgate.domains.audit.dto.Target
import com.devgate.domains.audit.models.Action
import com.devgate.domains.audit.services.AuditLogService
import com.devgate.security.AuthenticatedUser
import com.devgate.utils.Logger
import org.springframework.amqp.AmqpException
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class AuditLogServiceImpl
	@Autowired
	constructor(
		private val rabbitTemplate: RabbitTemplate,
		@Value($$"${rabbitmq.exchange}")
		private val exchange: String,
		@Value($$"${rabbitmq.routingKey}")
		private val routingKey: String
	) : AuditLogService {
		override fun sendMessage(
			action: Action,
			target: Target
		) {
			val payload =
				LogMessagePayload(
					action = action,
					actorId = currentActorId(),
					target = target
				)

			try {
				rabbitTemplate.convertAndSend(exchange, routingKey, payload)
			} catch (exception: AmqpException) {
				Logger.error(exception.stackTraceToString(), this)
			}
		}

		private fun currentActorId() =
			(SecurityContextHolder.getContext().authentication?.principal as? AuthenticatedUser)?.id
	}