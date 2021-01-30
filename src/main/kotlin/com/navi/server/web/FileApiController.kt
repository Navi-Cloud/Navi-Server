package com.navi.server.web

import com.navi.server.dto.FileResponseDTO
import com.navi.server.dto.FileSaveRequestDTO
import com.navi.server.service.FileService
import org.springframework.web.bind.annotation.*
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

}