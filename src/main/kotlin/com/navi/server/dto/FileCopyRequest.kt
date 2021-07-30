package com.navi.server.dto

data class FileCopyRequest(
    val fromToken: String,
    val fromPrevToken: String,
    val toPrevToken: String,
    val newFileName: String
)
