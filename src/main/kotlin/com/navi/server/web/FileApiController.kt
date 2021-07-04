package com.navi.server.web

import com.navi.server.domain.FileObject
import com.navi.server.dto.CreateFolderRequestDTO
import com.navi.server.dto.RootTokenResponseDto
import com.navi.server.service.FileService
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody

@RestController
class FileApiController (val fileService: FileService){

//    @GetMapping("/api/navi/root-token")
//    fun findRootToken(@RequestHeader httpHeaders: HttpHeaders) : ResponseEntity<RootTokenResponseDto> {
//        // Invalid or Non-Token will be filtered through Spring Security.
//        val tokenList: List<String> = httpHeaders["X-AUTH-TOKEN"]!!
//        return ResponseEntity.ok(
//            fileService.findRootToken(tokenList[0])
//        )
//    }
//
//    @GetMapping("/api/navi/files/list/{token}")
//    fun findInsideFiles(@RequestHeader httpHeaders: HttpHeaders, @PathVariable token: String) : ResponseEntity<List<FileObject>> {
//        // Invalid or Non-Token will be filtered through Spring Security.
//        val tokenList: List<String> = httpHeaders["X-AUTH-TOKEN"]!!
//
//        return fileService.findInsideFiles(tokenList[0], token)
//    }
//
//    @PostMapping("/api/navi/files")
//    fun fileUpload(@RequestHeader httpHeaders: HttpHeaders, @RequestPart("uploadFile") file: MultipartFile, @RequestPart("uploadPath") token: String)
//    : ResponseEntity<FileObject> {
//        // Invalid or Non-Token will be filtered through Spring Security.
//        val tokenList: List<String> = httpHeaders["X-AUTH-TOKEN"]!!
//
//        // when client requests, quotation marks(") are automatically inserted.
//        if(token.contains("\""))
//            return fileService.fileUpload(tokenList[0], token.substring(1, token.length - 1), file)
//        return fileService.fileUpload(tokenList[0], token, file)
//    }
//
//    @GetMapping("/api/navi/files")
//    fun fileDownload(@RequestHeader httpHeaders: HttpHeaders, @RequestParam("token") token: String, @RequestParam("prevToken") prevToken: String) : ResponseEntity<StreamingResponseBody> {
//        // Invalid or Non-Token will be filtered through Spring Security.
//        val tokenList: List<String> = httpHeaders["X-AUTH-TOKEN"]!!
//
//        return fileService.fileDownload(tokenList[0], token, prevToken)
//    }
//
//    @PostMapping("/api/navi/folder")
//    fun createNewFolder(@RequestHeader httpHeaders: HttpHeaders, @RequestBody createFolderRequest: CreateFolderRequestDTO)
//            : ResponseEntity<Unit>{
//        val tokenList: List<String> = httpHeaders["X-AUTH-TOKEN"]!!
//        fileService.createNewFolder(
//            userToken = tokenList[0],
//            parentFolderToken = createFolderRequest.parentFolderToken,
//            newFolderName = createFolderRequest.newFolderName
//        )
//        return ResponseEntity.noContent().build()
//    }
}