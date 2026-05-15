package dev.ktcloud.black.admin.api.gateway

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@ConfigurationPropertiesScan
@SpringBootApplication(scanBasePackages = ["dev.ktcloud.black"])
class AdminApiGatewayApplication


fun main(args: Array<String>) {
    runApplication<AdminApiGatewayApplication>(*args)
}
