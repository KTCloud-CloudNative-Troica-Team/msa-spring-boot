package dev.ktcloud.black.user.api.gateway.adapter.presentation.web.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsConfigurationSource
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebFluxSecurity
class SecurityConfig(
    private val jwtHeaderCheckFilter: JwtHeaderCheckFilter
) {
    @Bean
    fun filterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .cors { it.configurationSource(corsConfigurationSource()) }
            .csrf { it.disable() }
            .addFilterAt(jwtHeaderCheckFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            .authorizeExchange { exchange ->
                exchange
                    .pathMatchers(
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/webjars/**",
                        "/actuator/health",
                        "/actuator/info",
                    ).permitAll()
                    .pathMatchers(
                        "/api/v1/auth/signup",
                        "/api/v1/auth/signin",
                        "/api/v1/auth/check",
                    ).permitAll()
                    .pathMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/products/**").permitAll()
                    .pathMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/inventories/**").permitAll()
                    .anyExchange().authenticated()
            }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()

        configuration.allowedOrigins = listOf("http://localhost:5173", "http://127.0.0.1:5173")
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
        configuration.allowedHeaders = listOf("Authorization", "Content-Type", "Cache-Control")
        configuration.allowCredentials = true

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}