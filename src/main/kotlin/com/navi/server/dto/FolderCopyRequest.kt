package com.navi.server.dto

data class FolderCopyRequest (
    val fromToken: String,
    val fromPrevToken: String,
    val toPrevToken: String
)