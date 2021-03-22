package com.navi.server.service

import com.navi.server.domain.user.User
import com.navi.server.domain.user.UserTemplateRepository
import com.navi.server.dto.UserRegisterRequest
import com.navi.server.dto.UserRegisterResponse
import com.navi.server.error.exception.ConflictException
import com.navi.server.error.exception.UnknownErrorException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@Service
class UserService {
    @Autowired
    private lateinit var userTemplateRepository: UserTemplateRepository

    // TODO: Email Check
    fun registerUser(userRegisterRequest: UserRegisterRequest): ResponseEntity<UserRegisterResponse> {
        val user: User? = userTemplateRepository.findByUserName(userRegisterRequest.userName)

        if (user != null) {
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
}