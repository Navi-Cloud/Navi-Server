package com.navi.server.security

import com.navi.server.domain.user.UserTemplateRepository
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class CustomUserDetailService(private val userTemplateRepository: UserTemplateRepository) : UserDetailsService {
    override fun loadUserByUsername(userId: String): UserDetails {
        return userTemplateRepository.findByUserId(userId) ?: throw UsernameNotFoundException("Cannot find Users")
    }

}