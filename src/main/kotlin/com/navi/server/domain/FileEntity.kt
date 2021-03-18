package com.navi.server.domain

import org.springframework.data.mongodb.core.mapping.Document
import javax.persistence.*

@Document(collection = "files")
class FileEntity(
    var fileName: String,

    var fileType: String,

    var mimeType: String,

    var token: String,

    var prevToken: String,

    var lastModifiedTime: Long,

    var fileCreatedDate: String,

    var fileSize: String
){
    @Id
    lateinit var id : String
}
