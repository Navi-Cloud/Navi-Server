package com.navi.server.service

import com.navi.server.domain.user.User
import com.navi.server.domain.user.UserTemplateRepository
import com.navi.server.dto.UserRegisterRequest
import com.navi.server.dto.UserRegisterResponse
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.junit4.SpringRunner

@SpringBootTest
@RunWith(SpringRunner::class)
class UserServiceTest {
    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var userTemplateRepository: UserTemplateRepository

    @Before
    @After
    fun clearAllDB() {
        userTemplateRepository.clearAll()
    }

    @Test
    fun is_registerUser_returns_CONFLICT() {

        // Setup Data
        val mockUser: User = User(
            userName = "KangDroid",
            userPassword = "testingPassword",
            roles = setOf("ROLE_ADMIN")
        )
        userTemplateRepository.save(mockUser)

        // Save Check
        assertThat(userTemplateRepository.findByUserName(mockUser.userName)).isNotEqualTo(null)

        // Do work
        val userRegisterRequest: UserRegisterRequest = UserRegisterRequest(
            userName = "KangDroid",
            userPassword = "testingPassword",
            userEmail = "testingEmail"
        )

        runCatching {
            userService.registerUser(userRegisterRequest)
        }.onSuccess {
            fail("Should return duplicated exception!")
        }.onFailure {
            assertThat(it.message).isEqualTo("Username ${userRegisterRequest.userName} already exists!")
        }
    }

    @Test
    fun is_registerUser_returns_OK() {
        // Do work
        val userRegisterRequest: UserRegisterRequest = UserRegisterRequest(
            userName = "KangDroid",
            userPassword = "testingPassword",
            userEmail = "testingEmail"
        )

        runCatching {
            userService.registerUser(userRegisterRequest)
        }.onSuccess {
            assertThat(it.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(it.hasBody()).isEqualTo(true)
            assertThat(it.body.registeredName).isEqualTo(userRegisterRequest.userName)
            assertThat(it.body.registeredEmail).isEqualTo(userRegisterRequest.userEmail)
        }.onFailure {
            fail("This should returned OK!: ${it.stackTraceToString()}")
        }
    }
}