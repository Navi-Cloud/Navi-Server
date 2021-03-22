package com.navi.server.dto

class LoginRequest(
    val userName: String,
    val userPassword: String
)

class LoginResponse(
    val userToken: String
)