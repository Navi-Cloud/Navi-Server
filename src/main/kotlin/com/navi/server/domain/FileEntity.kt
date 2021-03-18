package com.navi.server.domain

import org.springframework.data.mongodb.core.mapping.Document
import javax.persistence.*

@Document(collection = "files")
class FileEntity(
    //@Column(length = 500, nullable = false)
    var fileName: String,

    //@Column(length = 500, nullable = false)
    var fileType: String,

    //@Column(length = 500, nullable = false)
    var mimeType: String,

    //@Column(length = 500, nullable = false)
    var token: String,

    //@Column(length = 500, nullable = false)
    var prevToken: String,

    //@Column(length = 500, nullable = false)
    var lastModifiedTime: Long,

    //@Column(length = 500, nullable = false)
    var fileCreatedDate: String,

    //@Column(length = 500, nullable = false)
    var fileSize: String
){
    @Id
    lateinit var id : String
}
