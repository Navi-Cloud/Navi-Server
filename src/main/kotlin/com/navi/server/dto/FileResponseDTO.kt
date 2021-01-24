package com.navi.server.dto

import com.navi.server.domain.FileEntity

class FileResponseDTO(
    var id: Long = 0,
    var fileName: String,
    var fileType: String,
    var nextToken: String,
    var lastModifiedTime: String,
) {

    constructor(entity: FileEntity): this(
        id = entity.id,
        fileName = entity.fileName,
        fileType = entity.fileType,
        nextToken = entity.nextToken,
        lastModifiedTime = entity.lastModifiedTime
    )

    fun toEntity(): FileEntity {
        return FileEntity(
            id = this.id,
            fileName = this.fileName,
            fileType = this.fileType,
            nextToken = this.nextToken,
            lastModifiedTime = this.lastModifiedTime
        )
    }
}