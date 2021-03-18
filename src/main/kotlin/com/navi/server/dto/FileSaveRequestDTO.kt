package com.navi.server.dto

import com.navi.server.domain.FileEntity

class FileSaveRequestDTO(
    var fileName: String,
    var fileType: String,
    var mimeType: String,
    var token: String,
    var prevToken: String,
    var lastModifiedTime: Long,
    var fileCreatedDate: String,
    var fileSize: String
) {

    constructor(responseDTO: FileResponseDTO): this(
        fileName = responseDTO.fileName,
        fileType = responseDTO.fileType,
        mimeType = responseDTO.mimeType,
        token = responseDTO.token,
        prevToken = responseDTO.prevToken,
        lastModifiedTime = responseDTO.lastModifiedTime,
        fileCreatedDate = responseDTO.fileCreatedDate,
        fileSize = responseDTO.fileSize
    )

    fun toEntity(): FileEntity {
        return FileEntity(
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