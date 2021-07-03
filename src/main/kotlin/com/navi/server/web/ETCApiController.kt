package com.navi.server.web

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class ETCApiController {
    @Autowired
    private lateinit var environment: Environment

    @GetMapping("/profile")
    fun getCurrentProfile(): ResponseEntity<String> {
        return ResponseEntity.ok(
            environment.activeProfiles[0]
        )
    }
}