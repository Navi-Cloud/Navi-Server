package com.navi.server.dto

import com.navi.server.domain.FileEntity

class FileSaveRequestDTO(
    var id: Long = 0,
    var fileName: String,
    var fileType: String,
    var mimeType: String,
    var token: String,
    var prevToken: String,
    var lastModifiedTime: Long,
    var fileCreatedDate: String,
    var fileSize: String
) {

    fun toEntity(): FileEntity {
        return FileEntity(
            id = this.id,
            fileName = this.fileName,
            fileType = this.fileType,
            mimeType = this.mimeType,
            token = this.token,
            prevToken = this.prevToken,
            lastModifiedTime = this.lastModifiedTime,
            fileCreatedDate = this.fileCreatedDate,
            fileSize = this.fileSize
        )
    }
}