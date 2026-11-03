package com.jfmart.backend.auth

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.util.Date

/** Identity carried by a validated token; available as `authentication.principal` in `@PreAuthorize`. */
data class AuthPrincipal(val id: Long, val username: String, val role: Role, val clientId: Long?)

@Service
class JwtService(
    @Value("\${jfmart.jwt.secret}") secret: String,
    @Value("\${jfmart.jwt.expiration-minutes}") private val expirationMinutes: Long
) {
    private val key = run {
        require(secret.toByteArray().size >= 32) { "jfmart.jwt.secret (JWT_SECRET) must be at least 32 bytes" }
        Keys.hmacShaKeyFor(secret.toByteArray())
    }

    fun issue(user: User): String {
        val now = Instant.now()
        return Jwts.builder()
            .subject(user.username)
            .claim("uid", user.id)
            .claim("role", user.role.name)
            .claim("cid", user.clientId)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(Duration.ofMinutes(expirationMinutes))))
            .signWith(key)
            .compact()
    }

    /** Returns null if the token is invalid or expired. */
    fun parse(token: String): AuthPrincipal? = try {
        val claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload
        AuthPrincipal(
            id = (claims["uid"] as Number).toLong(),
            username = claims.subject,
            role = Role.valueOf(claims["role"] as String),
            clientId = (claims["cid"] as Number?)?.toLong()
        )
    } catch (e: Exception) {
        null
    }
}
