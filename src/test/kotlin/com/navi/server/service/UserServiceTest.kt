package com.navi.server.service

import com.navi.server.component.FileConfigurationComponent
import com.navi.server.domain.user.User
import com.navi.server.domain.user.UserTemplateRepository
import com.navi.server.dto.LoginRequest
import com.navi.server.dto.UserRegisterRequest
import com.navi.server.error.exception.NotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.test.context.junit4.SpringRunner
import java.io.File

@SpringBootTest
@RunWith(SpringRunner::class)
class UserServiceTest {
    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var userTemplateRepository: UserTemplateRepository

    @Autowired
    private lateinit var fileConfigurationComponent: FileConfigurationComponent

    private lateinit var trashRootObject: File

    @Before
    fun initEnvironment() {
        fileConfigurationComponent.serverRoot = File(System.getProperty("java.io.tmpdir"), "naviServerTesting").absolutePath
        // Create trash directory
        trashRootObject = File(fileConfigurationComponent.serverRoot)
        trashRootObject.mkdir()
    }

    @After
    fun clearAllDB() {
        if (trashRootObject.exists()) {
            trashRootObject.deleteRecursively()
        }
        userTemplateRepository.clearAll()
    }

    @Test
    fun is_registerUser_returns_CONFLICT() {
        // Setup Data
        val mockUser: User = User(
            userId = "kangDroid",
            userName = "KangDroid",
            userEmail = "user@gmail.com",
            userPassword = "testingPassword",
            roles = setOf("ROLE_ADMIN")
        )
        userTemplateRepository.save(mockUser)

        // Save Check
        assertThat(userTemplateRepository.findByUserId(mockUser.userId)).isNotEqualTo(null)

        // Do work
        val userRegisterRequest: UserRegisterRequest = UserRegisterRequest(
            userId = "kangDroid",
            userName = "KangDroid",
            userEmail = "user@gmail.com",
            userPassword = "testingPassword",
        )

        runCatching {
            userService.registerUser(userRegisterRequest)
        }.onSuccess {
            fail("Should return duplicated exception!")
        }.onFailure {
            assertThat(it.message).isEqualTo("User id ${userRegisterRequest.userId} already exists!")
        }
    }

    @Test
    fun is_registerUser_returns_OK() {
        // Do work
        val userRegisterRequest: UserRegisterRequest = UserRegisterRequest(
            userId = "kangDroid",
            userName = "KangDroid",
            userPassword = "testingPassword",
            userEmail = "test@test.com"
        )

        runCatching {
            userService.registerUser(userRegisterRequest)
        }.onSuccess {
            assertThat(it.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(it.hasBody()).isEqualTo(true)
            assertThat(it.body!!.registeredId).isEqualTo(userRegisterRequest.userId)
            assertThat(it.body!!.registeredEmail).isEqualTo(userRegisterRequest.userEmail)
        }.onFailure {
            fail("This should returned OK!: ${it.stackTraceToString()}")
        }
    }

    @Test
    fun is_loginUser_returns_FORBIDDEN_no_username() {
        runCatching{
            userService.loginUser(
                LoginRequest(
                    userId = "whatever",
                    userPassword = "testPassword"
                )
            )
        }.onSuccess {
            fail("DB Should be empty, thus this test should not be succeed.")
        }.onFailure {
            assertThat(it is NotFoundException).isEqualTo(true)
        }
    }

    @Test
    fun is_loginUser_returns_FORBIDDEN_wrong_password() {
        // Setup Data
        val mockUser: User = User(
            userId = "kangdroid",
            userName = "KangDroid",
            userPassword = "testingPassword",
            roles = setOf("ROLE_ADMIN")
        )
        userTemplateRepository.save(mockUser)

        // Login
        runCatching {
            userService.loginUser(
                LoginRequest(
                    userId = "kangdroid",
                    userPassword = "testingPassword2"
                )
            )
        }.onSuccess {
            fail("Wrong password, but somehow it succeed?")
        }.onFailure {
            assertThat(it.message).isEqualTo("Username OR Password is wrong!")
        }
    }

    @Test
    fun is_loginUser_returns_OK() {
        // Setup Data
        val mockUser: User = User(
            userId = "kangdroid",
            userName = "KangDroid",
            userPassword = "testingPassword",
            roles = setOf("ROLE_ADMIN")
        )
        userTemplateRepository.save(mockUser)

        // Login
        runCatching {
            userService.loginUser(
                LoginRequest(
                    userId = "kangdroid",
                    userPassword = "testingPassword"
                )
            )
        }.onSuccess {
            assertThat(it.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(it.hasBody()).isEqualTo(true)
            assertThat(it.body!!.userToken).isNotEqualTo("")
        }.onFailure {
            fail("Wrong password, but somehow it succeed?")
        }
    }
}