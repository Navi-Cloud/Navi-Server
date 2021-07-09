package com.navi.server.web

import com.navi.server.domain.user.User
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.remove
import org.springframework.data.mongodb.gridfs.GridFsTemplate
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class ETCApiController(
    private val gridFsTemplate: GridFsTemplate,
    private val mongoTemplate: MongoTemplate
) {
    @Autowired
    private lateinit var environment: Environment

    @GetMapping("/api/remove")
    fun removeAllData(): String {
        gridFsTemplate.delete(Query())
        mongoTemplate.remove<User>(Query())
        return "OK"
    }

    @GetMapping("/profile")
    fun getCurrentProfile(): ResponseEntity<String> {
        return ResponseEntity.ok(
            environment.activeProfiles[0]
        )
    }
}