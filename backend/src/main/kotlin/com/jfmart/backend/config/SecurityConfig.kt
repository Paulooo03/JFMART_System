package com.jfmart.backend.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableMethodSecurity
class SecurityConfig(private val jwtAuthFilter: JwtAuthFilter) {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .logout { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .exceptionHandling { it.authenticationEntryPoint(HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)) }
            .headers { headers ->
                // The admin site only loads its own files; this also stops injected text from running as script
                headers.contentSecurityPolicy {
                    it.policyDirectives(
                        "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; " +
                            "connect-src 'self'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'"
                    )
                }
                headers.frameOptions { it.deny() }
            }
            .authorizeHttpRequests {
                // Error responses (400/403/409...) are dispatched to /error; without this they all become 401
                it.requestMatchers("/error").permitAll()
                // The admin web pages are plain files; the data behind them is protected by /api/admin/**
                it.requestMatchers("/", "/admin", "/admin/", "/admin/**").permitAll()
                it.requestMatchers("/api/auth/**").permitAll()
                it.requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")
                it.requestMatchers("/api/courier/**").hasAuthority("ROLE_DELIVERY")
                it.anyRequest().authenticated()
            }
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }
}
