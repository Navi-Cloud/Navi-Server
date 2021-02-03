package com.navi.server.domain

import javax.persistence.*

@Entity
class FileEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = Long.MAX_VALUE,

    @Column(length = 500, nullable = false)
    var fileName: String,

    @Column(length = 500, nullable = false)
    var fileType: String,

    @Column(length = 500, nullable = false)
    var mimeType: String,

    @Column(length = 500, nullable = false)
    var token: String,

    @Column(length = 500, nullable = false)
    var prevToken: String,

    @Column(length = 500, nullable = false)
    var lastModifiedTime: Long,

    @Column(length = 500, nullable = false)
    var fileCreatedDate: String,

    @Column(length = 500, nullable = false)
    var fileSize: String
)
