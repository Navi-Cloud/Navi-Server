package com.navi.server.dto

class UserRegisterRequest(
    val userId: String,
    val userName: String,
    val userEmail: String,
    val userPassword: String
)