package com.navi.server.web

import com.navi.server.domain.FileObject
import com.navi.server.dto.FileResponseDTO
import com.navi.server.service.FileService
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody

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

    @PostMapping("/api/navi/files")
    fun fileUpload(@RequestHeader httpHeaders: HttpHeaders, @RequestPart("uploadFile") file: MultipartFile, @RequestPart("uploadPath") token: String)
    : ResponseEntity<FileObject> {
        // Invalid or Non-Token will be filtered through Spring Security.
        val tokenList: List<String> = httpHeaders["X-AUTH-TOKEN"]!!

        // when client requests, quotation marks(") are automatically inserted.
        if(token.contains("\""))
            return fileService.fileUpload(tokenList[0], token.substring(1, token.length - 1), file)
        return fileService.fileUpload(tokenList[0], token, file)
    }

    @GetMapping("api/navi/files/{token}")
    fun fileDownload(@RequestHeader httpHeaders: HttpHeaders, @PathVariable token: String) : ResponseEntity<StreamingResponseBody> {
        // Invalid or Non-Token will be filtered through Spring Security.
        val tokenList: List<String> = httpHeaders["X-AUTH-TOKEN"]!!

        return fileService.fileDownload(tokenList[0], token)
    }
}