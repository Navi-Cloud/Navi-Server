package com.navi.server.service

import com.navi.server.domain.FileEntity
import com.navi.server.domain.FileRepository
import com.navi.server.dto.FileResponseDTO
import com.navi.server.dto.FileSaveRequestDTO
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.util.ArrayList
import java.util.stream.Collectors
import javax.xml.bind.DatatypeConverter
import kotlin.math.log
import kotlin.math.pow

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

    fun deleteByToken(token: String): Long {
        return fileRepository.deleteByToken(token)
    }

    fun findByToken(token: String): FileResponseDTO {
        return FileResponseDTO(fileRepository.findByToken(token))
    }

    var rootToken: String? = null

    fun getSHA256(input: String): String {
        val messageDigest: MessageDigest = MessageDigest.getInstance("SHA-256").also {
            it.update(input.toByteArray())
        }
        return DatatypeConverter.printHexBinary(messageDigest.digest())
    }

    fun convertSize(fileSize: Long): String {
        val fileUnit: String = "KMGTE"
        val logValue: Int = log(fileSize.toDouble(), 1024.0).toInt()
        if (logValue == 0 || fileSize == 0.toLong()) {
            return "${fileSize}B"
        }
        val calculatedValue: Double = fileSize / 1024.0.pow(logValue)
        return String.format("%.1f%ciB", calculatedValue, fileUnit[logValue-1])
    }
}