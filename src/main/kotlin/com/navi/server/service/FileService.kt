package com.navi.server.service

import com.navi.server.domain.FileEntity
import com.navi.server.domain.FileRepository
import com.navi.server.dto.FileResponseDTO
import com.navi.server.dto.FileSaveRequestDTO
import com.navi.server.error.exception.FileIOException
import com.navi.server.error.exception.NotFoundException
import com.navi.server.error.exception.UnknownErrorException
import org.apache.tika.Tika
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.lang.Exception
import java.net.URLEncoder
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.stream.Collectors
import javax.xml.bind.DatatypeConverter
import kotlin.math.log
import kotlin.math.pow

@Service
class FileService(val fileRepository: FileRepository) {
    fun findAllDesc(): ResponseEntity<List<FileResponseDTO>> {
        lateinit var fileList : List<FileEntity>
        runCatching {
            fileList = fileRepository.findAllDesc()
        }.onFailure {
            throw UnknownErrorException("Unknown Exception : cannot find files")
        }
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(fileList.stream()
                .map { FileResponseDTO(it) }
                .collect(Collectors.toList()))
    }

    fun save(fileSaveRequestDTO: FileSaveRequestDTO): ResponseEntity<Long> {
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(fileRepository.save(fileSaveRequestDTO.toEntity()).id)
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

    fun findInsideFiles(token: String): ResponseEntity<List<FileResponseDTO>> {
        runCatching {
            //check if this token is invalid
            findByToken(token)
        }.onFailure {
            throw NotFoundException("Cannot find file by this token : $token")
        }
        var result : List<FileEntity> = fileRepository.findInsideFiles(token)
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(result.stream()
                .map { FileResponseDTO(it) }
                .collect(Collectors.toList()))
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

    fun fileUpload(token: String, files: MultipartFile) : ResponseEntity<Long> {
        // find absolutePath from token
        lateinit var uploadFolderPath : String
        runCatching {
            uploadFolderPath = fileRepository.findByToken(token).fileName
        }.onFailure {
            throw NotFoundException("Cannot find file by this token : $token")
        }

        // upload
        // If the destination file already exists, it will be deleted first.
        lateinit var uploadFile : File
        runCatching {
            uploadFile = File(uploadFolderPath, files.originalFilename)
            files.transferTo(uploadFile)
        }.onFailure {
            when(it){
                is IOException -> throw FileIOException("File IO Exception")
                else -> throw UnknownErrorException("Unknown Exception : ${it.message}")
            }
        }

        // upload to DB
        val basicFileAttribute: BasicFileAttributes = Files.readAttributes(uploadFile.toPath(), BasicFileAttributes::class.java)
        val simpleDateFormat: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd-HH:mm:ss")

       var fileSaveRequestDTO = FileSaveRequestDTO (
            fileName = uploadFile.absolutePath,
            // Since we are not handling folder[recursive] upload/download, its type must be somewhat non-folder
            fileType = "File",
            mimeType =
                try {
                    tika.detect(uploadFile)
                } catch (e: Exception) {
                    println("Failed to detect mimeType for: ${e.message}")
                    "File"
                }
            ,
            token = getSHA256(uploadFile.absolutePath),
            prevToken = getSHA256(uploadFolderPath),
            lastModifiedTime = uploadFile.lastModified(),
            fileCreatedDate = simpleDateFormat.format(basicFileAttribute.creationTime().toMillis()),
            fileSize = convertSize(basicFileAttribute.size())
        )
        return this.save(fileSaveRequestDTO)
    }

    fun fileDownload(token: String) : ResponseEntity<Resource> {
        lateinit var file : FileEntity
        lateinit var resource: Resource
        runCatching {
            file = fileRepository.findByToken(token)
            resource = InputStreamResource(Files.newInputStream(Paths.get(file.fileName)))
        }.onFailure {
            when(it){
                is EmptyResultDataAccessException -> throw NotFoundException("Cannot find file by this token : $token")
                is NoSuchFileException -> throw NotFoundException("File Not Found !")
                else -> throw UnknownErrorException("Unknown Exception : ${it.message}")
            }
        }
        val fileResponseDTO = FileResponseDTO(file).apply {
            val osType: String = System.getProperty("os.name").toLowerCase()
            if (osType.indexOf("win") >= 0) {
                this.fileName = this.fileName.split("\\").last()
            } else {
                this.fileName = this.fileName.split("/").last()
            }
        }
        val again: String = String.format("attachment; filename=\"%s\"", URLEncoder.encode(fileResponseDTO.fileName, "UTF-8"))
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/octet-stream"))
            .header(HttpHeaders.CONTENT_DISPOSITION, again)
            .body(resource)
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