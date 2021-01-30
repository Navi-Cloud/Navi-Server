package com.navi.server.dto

import com.navi.server.domain.FileEntity

class FileSaveRequestDTO(
    var id: Long = 0,
    var fileName: String,
    var fileType: String,
    var nextToken: String,
    var prevToken: String,
    var lastModifiedTime: String,
) {

    fun toEntity(): FileEntity {
        return FileEntity(
            id = this.id,
            fileName = this.fileName,
            fileType = this.fileType,
            nextToken = this.nextToken,
            prevToken = this.prevToken,
            lastModifiedTime = this.lastModifiedTime
        )
    }
}