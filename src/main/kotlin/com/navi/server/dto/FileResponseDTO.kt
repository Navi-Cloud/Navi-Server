package com.navi.server.dto

import com.navi.server.domain.FileEntity

class FileResponseDTO(
    var id: Long = 0,
    var fileName: String,
    var fileType: String,
    var token: String,
    var prevToken: String,
    var lastModifiedTime: Long,
    var fileCreatedDate: String,
    var fileSize: String
) {

    constructor(entity: FileEntity): this(
        id = entity.id,
        fileName = entity.fileName,
        fileType = entity.fileType,
        token = entity.token,
        prevToken = entity.prevToken,
        lastModifiedTime = entity.lastModifiedTime,
        fileCreatedDate = entity.fileCreatedDate,
        fileSize = entity.fileSize
    )

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

    override fun toString(): String {
        return "ID: $id\n" +
                "File Name:\t $fileName\n" +
                "File Type:\t $fileType\n" +
                "Next Token:\t $token\n" +
                "Prev Token:\t $prevToken\n" +
                "LMT:\t $lastModifiedTime\n" +
                "File Created:\t $fileCreatedDate" +
                "File Size:\t $fileSize"
    }
}