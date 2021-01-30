package com.navi.server.service

import com.navi.server.domain.FileEntity
import com.navi.server.domain.FileRepository
import com.navi.server.dto.FileResponseDTO
import com.navi.server.dto.FileSaveRequestDTO
import org.springframework.stereotype.Service
import java.util.ArrayList
import java.util.stream.Collectors

@Service
class FileService(val fileRepository: FileRepository) {
    fun findAllDesc(): List<FileResponseDTO> {
        return fileRepository.findAllDesc().stream()
            .map {FileResponseDTO(it)}
            .collect(Collectors.toList())
    }

    fun save(fileSaveRequestDTO: FileSaveRequestDTO): Long {
        return fileRepository.save(fileSaveRequestDTO.toEntity()).id
    }

    fun saveAll(inputList: List<FileSaveRequestDTO>, digestValue: Int = 10000) {

        val tmpList: ArrayList<FileEntity> = ArrayList()
        inputList.forEach { listB ->
            tmpList.add(listB.toEntity())
            if (tmpList.size == digestValue) {
                fileRepository.saveAll(tmpList)
                tmpList.clear()
            }
        }

        if (tmpList.size > 0) {
            fileRepository.saveAll(tmpList)
        }
    }

    fun findInsideFiles(token: String) : List<FileResponseDTO> {
        return fileRepository.findInsideFiles(token).stream()
            .map { FileResponseDTO(it) }
            .collect(Collectors.toList())
    }

    var rootToken: String? = null

}