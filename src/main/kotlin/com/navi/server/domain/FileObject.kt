package com.navi.server.domain

class FileObject(
    var fileName: String,

    var fileType: String,

    var mimeType: String,

    var token: String,

    var prevToken: String,

    var lastModifiedTime: Long,

    var fileCreatedDate: String,

    var fileSize: String
)