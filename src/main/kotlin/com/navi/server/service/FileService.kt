package com.navi.server.service

import com.navi.server.domain.FileEntity
import com.navi.server.domain.FileRepository
import com.navi.server.dto.FileResponseDTO
import com.navi.server.dto.FileSaveRequestDTO
import org.apache.tika.Tika
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.io.FileNotFoundException
import java.lang.Exception
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import java.util.stream.Collectors
import javax.xml.bind.DatatypeConverter
import kotlin.math.log
import kotlin.math.pow

@Service
class FileService(val fileRepository: FileRepository) {
    fun findAllDesc(): List<FileResponseDTO> {
        return fileRepository.findAllDesc().stream()
            .map { FileResponseDTO(it) }
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

    fun findInsideFiles(token: String): List<FileResponseDTO> {
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

    var rootPath: String? = null
    var rootToken: String? = null
    val tika = Tika()

    fun fileUpload(token: String, files: MultipartFile) : Long {
        try {
            // find absolutePath from token
            val uploadFolderPath = fileRepository.findByToken(token).fileName

            // upload
            // If the destination file already exists, it will be deleted first.
            val uploadFile = File(uploadFolderPath, files.originalFilename)
            println("uploadFilePath -> ${uploadFile.absolutePath}")
            files.transferTo(uploadFile)

            // upload to DB
            val basicFileAttribute: BasicFileAttributes = Files.readAttributes(uploadFile.toPath(), BasicFileAttributes::class.java)
            val simpleDateFormat: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd-HH:mm:ss")

            var fileSaveRequestDTO = FileSaveRequestDTO (
                fileName = uploadFile.absolutePath,
                fileType = if (uploadFile.isDirectory) "Folder" else "File",
                mimeType = if (uploadFile.isDirectory) "Folder" else {
                    try {
                        tika.detect(uploadFile)
                    } catch (e: Exception) {
                        println("Failed to detect mimeType for: ${e.message}")
                        "File"
                    }
                },
                token = getSHA256(uploadFile.absolutePath),
                prevToken = getSHA256(rootPath?.let { rootPath } ?: "rootToken"),
                lastModifiedTime = uploadFile.lastModified(),
                fileCreatedDate = simpleDateFormat.format(basicFileAttribute.creationTime().toMillis()),
                fileSize = convertSize(basicFileAttribute.size())
                )
            return this.save(fileSaveRequestDTO)
        } catch (e: Exception){
            e.printStackTrace()
        }
        return -1
    }
    fun fileDownload(token: String) : Pair<FileResponseDTO?, Resource?> {
        val file : FileEntity = fileRepository.findByToken(token)
        try {
            val resource: Resource? = InputStreamResource(Files.newInputStream(Paths.get(file.fileName)))
            return Pair(FileResponseDTO(file), resource)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace();
        }
        return Pair(null, null)
    }

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
        return String.format("%.1f%ciB", calculatedValue, fileUnit[logValue - 1])
    }
}