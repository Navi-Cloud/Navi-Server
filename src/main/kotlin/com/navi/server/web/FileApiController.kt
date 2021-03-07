package com.navi.server.web

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
    fun findRootToken() : ResponseEntity<String> {
        return fileService.rootToken?.let {
            ResponseEntity
                .status(HttpStatus.OK)
                .body(fileService.rootToken)
        } ?: throw UnknownErrorException("serverRoot does not exist!")
    }

    @GetMapping("/api/navi/files/list")
    fun findAllDesc() : ResponseEntity<List<FileResponseDTO>>{
        return fileService.findAllDesc()
    }

    @GetMapping("/api/navi/files/list/{token}")
    fun findInsideFiles(@PathVariable token: String) : ResponseEntity<List<FileResponseDTO>> {
        return fileService.findInsideFiles(token)
    }

    @PostMapping("/api/navi/files")
    fun fileUpload(@RequestPart("uploadFile") file: MultipartFile, @RequestPart("uploadPath") token: String)
    : ResponseEntity<Long> {
        // when client requests, quotation marks(") are automatically inserted.
        if(token.contains("\""))
            return fileService.fileUpload(token.substring(1, token.length - 1), file)
        return fileService.fileUpload(token, file)
    }

    @GetMapping("api/navi/files/{token}")
    fun fileDownload(@PathVariable token: String) : ResponseEntity<Resource> {
        val pair : Pair<FileResponseDTO, Resource> = fileService.fileDownload(token) ?: run {
            throw NotFoundException("File Not Found")
        }
        val fileResponseDTO: FileResponseDTO = pair.first.apply {
            val osType: String = System.getProperty("os.name").toLowerCase()

            if (osType.indexOf("win") >= 0) {
                this.fileName = this.fileName.split("\\").last()
            } else {
                this.fileName = this.fileName.split("/").last()
            }
        }

        val again: String = String.format("attachment; filename=\"%s\"", URLEncoder.encode(fileResponseDTO.fileName, "UTF-8"))
        val resource: Resource = pair.second
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/octet-stream"))
                .header(HttpHeaders.CONTENT_DISPOSITION, again)
                .body(resource)
    }
}