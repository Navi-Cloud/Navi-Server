package com.navi.server.service

import com.navi.server.domain.user.User
import com.navi.server.domain.user.UserTemplateRepository
import com.navi.server.dto.LoginRequest
import com.navi.server.dto.LoginResponse
import com.navi.server.dto.UserRegisterRequest
import com.navi.server.dto.UserRegisterResponse
import com.navi.server.error.exception.ConflictException
import com.navi.server.error.exception.ForbiddenException
import com.navi.server.error.exception.UnknownErrorException
import com.navi.server.security.JWTTokenProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@Service
class UserService {
    @Autowired
    private lateinit var userTemplateRepository: UserTemplateRepository

    @Autowired
    private lateinit var jwtTokenProvider: JWTTokenProvider

    // TODO: Email Check
    fun registerUser(userRegisterRequest: UserRegisterRequest): ResponseEntity<UserRegisterResponse> {
        runCatching {
            userTemplateRepository.findByUserName(userRegisterRequest.userName)
        }.onSuccess {
            throw ConflictException("Username ${userRegisterRequest.userName} already exists!")
        }

        userTemplateRepository.save(
            User(
                userName = userRegisterRequest.userName,
                userPassword = userRegisterRequest.userPassword,
                roles = setOf("ROLE_ADMIN")
            )
        )

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(
                UserRegisterResponse(
                    registeredEmail = userRegisterRequest.userEmail,
                    registeredName = userRegisterRequest.userName
                )
            )
    }

    fun loginUser(userLoginRequest: LoginRequest): ResponseEntity<LoginResponse> {
        lateinit var user: User
        runCatching {
            userTemplateRepository.findByUserName(userLoginRequest.userName)
        }.onSuccess {
            user = it
        }.onFailure {
            throw it
        }

        if (user.userPassword != userLoginRequest.userPassword) {
            throw ForbiddenException("Username OR Password is wrong!")
        }

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(
                LoginResponse(
                    userToken = jwtTokenProvider.createToken(user.userName, user.roles.toList())
                )
            )
    }
}