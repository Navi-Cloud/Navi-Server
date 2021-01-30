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
    var nextToken: String,

    @Column(length = 500, nullable = false)
    var prevToken: String,

    @Column(length = 500, nullable = false)
    var lastModifiedTime: String
)
