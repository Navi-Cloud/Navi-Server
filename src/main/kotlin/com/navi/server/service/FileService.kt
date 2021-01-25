package com.navi.server.service

import com.navi.server.domain.FileRepository
import com.navi.server.dto.FileResponseDTO
import com.navi.server.dto.FileSaveRequestDTO
import org.springframework.stereotype.Service
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
}