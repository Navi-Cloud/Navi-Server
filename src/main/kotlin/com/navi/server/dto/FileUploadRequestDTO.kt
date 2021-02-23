package com.navi.server.dto

import org.springframework.web.multipart.MultipartFile

class FileUploadRequestDTO (
    var uploadPath: String,
    var uploadFile: MultipartFile
){

}