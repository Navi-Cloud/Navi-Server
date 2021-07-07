package com.navi.server.config

import com.navi.server.security.JWTTokenProvider
import com.navi.server.security.JwtAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.builders.WebSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.firewall.HttpFirewall
import org.springframework.security.web.firewall.StrictHttpFirewall


@EnableWebSecurity
class SecurityConfig(private val jwtTokenProvider: JWTTokenProvider) : WebSecurityConfigurerAdapter() {

    // Register authenticationManagerBean.
    @Bean
    override fun authenticationManagerBean(): AuthenticationManager {
        return super.authenticationManagerBean()
    }

    @Bean
    fun allowUrlEncodedSlashHttpFirewall(): HttpFirewall? {
        return StrictHttpFirewall().apply {
            setAllowUrlEncodedPercent(true)
        }
    }

    // Configure Http Security
    override fun configure(http: HttpSecurity) {
        http
            .csrf().disable()
            .headers().frameOptions().disable()
            .and()
            .authorizeRequests()
            .antMatchers("/api/navi/root-token").hasRole("USER")
            .antMatchers("/**").permitAll()
            .and()
            .addFilterBefore(
                JwtAuthenticationFilter(jwtTokenProvider),
                UsernamePasswordAuthenticationFilter::class.java
            )

    }
}