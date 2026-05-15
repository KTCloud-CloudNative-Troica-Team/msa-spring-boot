package dev.ktcloud.black.admin.api.gateway.adapter.presentation.web.configuration

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "admin")
data class AdminSecurityProperties(
    val requiredRole: String = "ADMIN",
)
