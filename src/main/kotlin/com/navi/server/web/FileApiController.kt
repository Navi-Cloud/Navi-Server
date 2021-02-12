package com.navi.server.web

import com.navi.server.dto.FileResponseDTO
import com.navi.server.dto.FileSaveRequestDTO
import com.navi.server.service.FileService
import org.apache.commons.logging.Log
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.stream.Collectors

@RestController
class FileApiController (val fileService: FileService){

    @PostMapping("/api/navi/files")
    fun save(@RequestBody fileSaveRequestDTO: FileSaveRequestDTO) : Long {
        return fileService.save(fileSaveRequestDTO)
    }

    @GetMapping("/api/navi/fileList")
    fun findAllDesc() : List<FileResponseDTO>{
        return fileService.findAllDesc()
    }

    @GetMapping("/api/navi/rootToken")
    fun findRootToken() : String{
        return fileService.rootToken ?: "serverRoot does not exist!"
    }

    @GetMapping("/api/navi/findInsideFiles/{token}")
    fun findInsideFiles(@PathVariable token: String) : List<FileResponseDTO> {
        return fileService.findInsideFiles(token)
    }

    @PostMapping("/api/navi/fileUpload")
    fun fileUpload(@RequestParam(value = "uploadFile") file: MultipartFile) : Long {
        println("file input ==> "+ file.originalFilename)
        return fileService.fileUpload(file)
    }

    @GetMapping("api/navi/fileDownload/{token}")
    fun fileDownload(@PathVariable token: String) : ResponseEntity<Resource>{
        val pair : Pair<FileResponseDTO?, Resource?> = fileService.fileDownload(token)
        val fileResponseDTO: FileResponseDTO? = pair.first
        val originalFilename =
            fileResponseDTO?.let {
                fileResponseDTO.fileName.split("\\").last()
            } ?: "tmp"

        val resource: Resource? = pair.second
        return resource?.let {
            ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/octet-stream"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$originalFilename\"")
                .body(resource)
        } ?: ResponseEntity.badRequest().body(null);
    }
}