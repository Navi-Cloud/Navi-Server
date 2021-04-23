package com.navi.server.dto

class LoginRequest(
    val userId: String,
    val userPassword: String
)

class LoginResponse(
    val userToken: String
)