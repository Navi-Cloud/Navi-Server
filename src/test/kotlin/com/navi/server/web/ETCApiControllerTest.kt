package com.navi.server.web

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.getForEntity
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.junit4.SpringRunner

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner::class)
class ETCApiControllerTest {
    @LocalServerPort
    private var serverPort: Int? = null

    private val restTemplate: TestRestTemplate = TestRestTemplate()

    @Test
    fun is_getting_active_profile_works_well() {
        val serverUrl: String = "http://localhost:${serverPort}/profile"
        val responseEntity: ResponseEntity<String> = restTemplate.getForEntity(serverUrl)

        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(responseEntity.body).isEqualTo("default")
    }
}