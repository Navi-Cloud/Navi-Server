package com.navi.server.web

import com.navi.server.dto.LoginRequest
import com.navi.server.dto.LoginResponse
import com.navi.server.dto.UserRegisterRequest
import com.navi.server.dto.UserRegisterResponse
import com.navi.server.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class UserApiController (val userService: UserService){

    @PostMapping("/api/navi/join")
    fun registerUser(@RequestBody userRegisterRequest: UserRegisterRequest) : ResponseEntity<UserRegisterResponse> {
        return userService.registerUser(userRegisterRequest)
    }

    @PostMapping("/api/navi/login")
    fun loginUser(@RequestBody userLoginRequest: LoginRequest) : ResponseEntity<LoginResponse>{
        return userService.loginUser(userLoginRequest)
    }

}