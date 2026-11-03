package com.jfmart.backend.config

import com.jfmart.backend.auth.JwtService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthFilter(private val jwtService: JwtService) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain
    ) {
        val header = request.getHeader("Authorization")
        if (header != null && header.startsWith("Bearer ")) {
            jwtService.parse(header.removePrefix("Bearer ").trim())?.let { principal ->
                SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(
                    principal, null, listOf(SimpleGrantedAuthority(principal.role.name))
                )
            }
        }
        chain.doFilter(request, response)
    }
}
