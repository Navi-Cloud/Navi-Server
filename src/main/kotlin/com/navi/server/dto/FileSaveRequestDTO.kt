package com.navi.server.dto

import com.navi.server.domain.FileEntity

class FileSaveRequestDTO(
    var id: Long = 0,
    var fileName: String,
    var fileType: String,
    var token: String,
    var prevToken: String,
    var lastModifiedTime: String,
    var fileCreatedDate: String,
    var fileSize: Long
) {

    fun toEntity(): FileEntity {
        return FileEntity(
            id = this.id,
            fileName = this.fileName,
            fileType = this.fileType,
            token = this.token,
            prevToken = this.prevToken,
            lastModifiedTime = this.lastModifiedTime,
            fileCreatedDate = this.fileCreatedDate,
            fileSize = this.fileSize
        )
    }
}