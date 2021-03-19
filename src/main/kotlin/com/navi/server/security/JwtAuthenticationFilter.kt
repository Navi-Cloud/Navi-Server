package com.navi.server.security

import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.GenericFilterBean
import javax.servlet.FilterChain
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest

class JwtAuthenticationFilter(private val jwtTokenProvider: JWTTokenProvider) : GenericFilterBean() {
    override fun doFilter(request: ServletRequest?, response: ServletResponse?, chain: FilterChain?) {
        // Get Token from Header
        val token: String? = jwtTokenProvider.resolveToken(request as HttpServletRequest)

        // Check for whether token is correct or not
        if (token != null && jwtTokenProvider.validateToken(token)) {
            // Get User Information
            val authentication: Authentication = jwtTokenProvider.getAuthentication(token)

            // Save new authentication token to security context
            SecurityContextHolder.getContext().authentication = authentication
        }
        chain?.doFilter(request, response)
    }
}