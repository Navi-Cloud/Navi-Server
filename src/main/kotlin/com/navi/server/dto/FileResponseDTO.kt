package com.navi.server.dto

import com.navi.server.domain.FileEntity

class FileResponseDTO(
    var id: Long = 0,
    var fileName: String,
    var fileType: String,
    var nextToken: String,
    var prevToken: String,
    var lastModifiedTime: String,
) {

    constructor(entity: FileEntity): this(
        id = entity.id,
        fileName = entity.fileName,
        fileType = entity.fileType,
        nextToken = entity.nextToken,
        prevToken = entity.prevToken,
        lastModifiedTime = entity.lastModifiedTime
    )

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