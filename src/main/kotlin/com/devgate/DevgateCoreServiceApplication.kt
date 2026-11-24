package com.devgate

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class DevgateCoreServiceApplication

fun main(args: Array<String>) {
	runApplication<DevgateCoreServiceApplication>(*args)
}