package com.navi.server.web

import com.navi.server.domain.FileEntity
import com.navi.server.dto.FileResponseDTO
import com.navi.server.error.exception.NotFoundException
import com.navi.server.error.exception.UnknownErrorException
import com.navi.server.service.FileService
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.net.URLEncoder

@RestController
class FileApiController (val fileService: FileService){

    @GetMapping("/api/navi/root-token")
    fun findRootToken(@RequestHeader httpHeaders: HttpHeaders) : ResponseEntity<String> {
        // Invalid or Non-Token will be filtered through Spring Security.
        val tokenList: List<String> = httpHeaders["X-AUTH-TOKEN"]!!
        return fileService.findRootToken(tokenList[0])
    }

    @GetMapping("/api/navi/files/list")
    fun findAllDesc(@RequestHeader httpHeaders: HttpHeaders) : ResponseEntity<List<FileResponseDTO>>{
        // Invalid or Non-Token will be filtered through Spring Security.
        val tokenList: List<String> = httpHeaders["X-AUTH-TOKEN"]!!

        return fileService.findAllDesc(tokenList[0])
    }

    @GetMapping("/api/navi/files/list/{token}")
    fun findInsideFiles(@RequestHeader httpHeaders: HttpHeaders, @PathVariable token: String) : ResponseEntity<List<FileResponseDTO>> {
        // Invalid or Non-Token will be filtered through Spring Security.
        val tokenList: List<String> = httpHeaders["X-AUTH-TOKEN"]!!

        return fileService.findInsideFiles(tokenList[0], token)
    }
//
//    @PostMapping("/api/navi/files")
//    fun fileUpload(@RequestPart("uploadFile") file: MultipartFile, @RequestPart("uploadPath") token: String)
//    : ResponseEntity<FileEntity> {
//        // when client requests, quotation marks(") are automatically inserted.
//        if(token.contains("\""))
//            return fileService.fileUpload(token.substring(1, token.length - 1), file)
//        return fileService.fileUpload(token, file)
//    }
//
//    @GetMapping("api/navi/files/{token}")
//    fun fileDownload(@PathVariable token: String) : ResponseEntity<Resource> {
//        return fileService.fileDownload(token)
//    }
}