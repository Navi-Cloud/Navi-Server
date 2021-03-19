package com.navi.server.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jws
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Component
import java.util.*
import javax.servlet.http.HttpServletRequest

@Component
class JWTTokenProvider(private val userDetailsService: UserDetailsService) {
    // TODO: Make it privately configured
    private val testInnerPassword: String = Base64.getEncoder()
        .encodeToString("alsdfj;alsdfjkldsajlksajdflasdjl;fjasldkfjlsadfas;dfjlasdjfsl;adfj;ldsjfklsajdfljs".toByteArray())

    // Expiration Time
    private val expirationPeriod: Long = 5 * 60 * 1000L

    // Create Token based on input information
    fun createToken(userPk: String, roles: List<String>): String {
        val claims: Claims = Jwts.claims().setSubject(userPk).apply {
            this.put("roles", roles)
        }
        val currentDate: Date = Date()
        return Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(currentDate)
            .setExpiration(Date(currentDate.time + expirationPeriod))
            .signWith(SignatureAlgorithm.HS256, testInnerPassword)
            .compact()
    }

    // Check for auth information in JWT Token
    fun getAuthentication(token: String): Authentication {
        val userDetails: UserDetails = userDetailsService.loadUserByUsername(getUserPk(token))

        return UsernamePasswordAuthenticationToken(
            userDetails, "", userDetails.authorities
        )
    }

    fun getUserPk(token: String): String {
        return Jwts.parser().setSigningKey(testInnerPassword).parseClaimsJws(token).body.subject
    }

    fun resolveToken(request: HttpServletRequest): String? {
        return request.getHeader("X-AUTH-TOKEN")
    }

    fun validateToken(jwtToken: String): Boolean {
        return kotlin.runCatching {
            val tmpClaims: Jws<Claims> = Jwts.parser().setSigningKey(testInnerPassword).parseClaimsJws(jwtToken)
            !tmpClaims.body.expiration.before(Date())
        }.getOrDefault(false)
    }
}