package com.navi.server.domain

class FileObject(
    val userId: String,
    val token: String,
    val prevToken: String,
    val fileName: String,
    val fileType: String
)