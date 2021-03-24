package com.navi.server.service

import com.navi.server.component.FileConfigurationComponent
import com.navi.server.domain.FileObject
import com.navi.server.domain.user.User
import com.navi.server.domain.user.UserTemplateRepository
import com.navi.server.dto.FileResponseDTO
import com.navi.server.dto.FileSaveRequestDTO
import com.navi.server.error.exception.FileIOException
import com.navi.server.error.exception.NotFoundException
import com.navi.server.error.exception.UnknownErrorException
import com.navi.server.security.JWTTokenProvider
import org.apache.tika.Tika
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.stream.Collectors
import javax.xml.bind.DatatypeConverter
import kotlin.math.log
import kotlin.math.pow

@Service
class FileService {
    @Autowired
    private lateinit var userTemplateRepository: UserTemplateRepository

    @Autowired
    private lateinit var jwtTokenProvider: JWTTokenProvider

    @Autowired
    private lateinit var fileConfigurationComponent: FileConfigurationComponent

    private val tika = Tika()

    private fun convertTokenToUserName(inputToken: String): String {
        var userName: String = ""
        runCatching {
            jwtTokenProvider.getUserPk(inputToken)
        }.onSuccess {
            userName = it
        }.onFailure {
            throw NotFoundException("Username is NOT Found!")
        }

        return userName
    }

    fun findRootToken(userToken: String): ResponseEntity<String> {
        val userName: String = convertTokenToUserName(userToken)
        val userFileObject: FileObject = userTemplateRepository.findByToken(userName, getSHA256("/"))

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(userFileObject.token)
    }

    fun findAllDesc(userToken: String): ResponseEntity<List<FileResponseDTO>> {
        val userName: String = convertTokenToUserName(userToken)

        val fileList: List<FileObject> = userTemplateRepository.findAllFileList(userName)
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(fileList.stream()
                .map { FileResponseDTO(it) }
                .collect(Collectors.toList()))
    }

    fun findInsideFiles(userToken: String, prevToken: String): ResponseEntity<List<FileResponseDTO>> {
        val userName: String = convertTokenToUserName(userToken)

        // Check if token is actually exists!
        userTemplateRepository.findByToken(userName, prevToken) // It will throw error when token is not acutally exists

        // Since User Name and prevToken is verified by above statement, any error from here will be
        // Internal Server error.
        val result: List<FileObject> = userTemplateRepository.findAllByPrevToken(userName, prevToken)

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(result.stream()
                .map { FileResponseDTO(it) }
                .collect(Collectors.toList()))
    }

    fun convertFileNameToFullPath(userName: String, filePath: String): String {
        val finalFilePath: Path = Paths.get(fileConfigurationComponent.serverRoot, userName, filePath)

        return finalFilePath.toFile().absolutePath
    }

    fun fileUpload(userToken: String, uploadFolderToken: String, files: MultipartFile): ResponseEntity<FileObject> {
        val userName:String = convertTokenToUserName(userToken)

        // find absolutePath from token
        val fileObject: FileObject = userTemplateRepository.findByToken(userName, uploadFolderToken)

        // Need to Con-cat string to real path
        val uploadFolderPath: String = convertFileNameToFullPath(userName, fileObject.fileName)

        // upload
        // If the destination file already exists, it will be deleted first.
        lateinit var uploadFile: File
        runCatching {
            uploadFile = File(uploadFolderPath, files.originalFilename)
            files.transferTo(uploadFile)
        }.onFailure {
            when (it) {
                is IOException -> throw FileIOException("File IO Exception")
                else -> throw UnknownErrorException("Unknown Exception : ${it.message}")
            }
        }

        // upload to DB
        val basicFileAttribute: BasicFileAttributes =
            Files.readAttributes(uploadFile.toPath(), BasicFileAttributes::class.java)
        val simpleDateFormat: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd-HH:mm:ss")


        val toSubstring: String = "${fileConfigurationComponent.serverRoot}/$userName"
        val saveFileObject: FileObject = FileObject(
            fileName = uploadFile.absolutePath.substring(toSubstring.length, uploadFile.absolutePath.length),
            // Since we are not handling folder[recursive] upload/download, its type must be somewhat non-folder
            fileType = "File",
            mimeType =
            try {
                tika.detect(uploadFile)
            } catch (e: Exception) {
                println("Failed to detect mimeType for: ${e.message}")
                "File"
            },
            token = getSHA256(uploadFile.absolutePath),
            prevToken = getSHA256(uploadFolderPath),
            lastModifiedTime = uploadFile.lastModified(),
            fileCreatedDate = simpleDateFormat.format(basicFileAttribute.creationTime().toMillis()),
            fileSize = convertSize(basicFileAttribute.size())
        )

        // Since above findByToken works, it means there is an user name.
        val user: User = userTemplateRepository.findByUserName(userName)
        user.fileList.add(saveFileObject)

        // TODO: Since loading whole user and re-saving whole user might be resource-heavy. Maybe creating another Query function to reduce them?
        userTemplateRepository.save(user) // Save to user!

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(saveFileObject)
    }

//
//    fun save(inputUserName: String, fileSaveRequestDTO: FileSaveRequestDTO): ResponseEntity<User> {
//        val user: User = userTemplateRepository.findByUserName(inputUserName)
//            ?: throw NotFoundException("Cannot find user: $inputUserName")
//        user.fileList.add(fileSaveRequestDTO.toEntityObject())
//        return ResponseEntity
//            .status(HttpStatus.OK)
//            .body(userTemplateRepository.save(user))
//    }
//
//
//    fun deleteByToken(inputUserName: String, fileToken: String): Boolean {
//        return userTemplateRepository.deleteByToken(inputUserName, fileToken).wasAcknowledged()
//    }
//
//    fun findByToken(inputUserName: String, fileToken: String): FileResponseDTO {
//        val result: FileObject = userTemplateRepository.findByToken(inputUserName, fileToken)
//            ?: throw NotFoundException("Cannot find file information with user: $inputUserName, and token: $fileToken")
//        return FileResponseDTO(result)
//    }
//
//    var rootPath: String? = null
//    var rootToken: String? = null
//    val tika = Tika()
//
//
//    fun fileDownload(inputUserName: String, fileToken: String): ResponseEntity<Resource> {
//        lateinit var file: FileObject
//        lateinit var resource: Resource
//        runCatching {
//            file = userTemplateRepository.findByToken(inputUserName, fileToken)
//                ?: throw NotFoundException("")
//            resource = InputStreamResource(Files.newInputStream(Paths.get(file.fileName)))
//        }.onFailure {
//            when (it) {
//                is EmptyResultDataAccessException -> throw NotFoundException("Cannot find file by this token : $fileToken")
//                is NoSuchFileException -> throw NotFoundException("File Not Found !")
//                is NotFoundException -> throw NotFoundException("Cannot find file by this token : $fileToken")
//                else -> throw UnknownErrorException("Unknown Exception : ${it.message}")
//            }
//        }
//        val fileResponseDTO = FileResponseDTO(file).apply {
//            val osType: String = System.getProperty("os.name").toLowerCase()
//            if (osType.indexOf("win") >= 0) {
//                this.fileName = this.fileName.split("\\").last()
//            } else {
//                this.fileName = this.fileName.split("/").last()
//            }
//        }
//        val again: String =
//            String.format("attachment; filename=\"%s\"", URLEncoder.encode(fileResponseDTO.fileName, "UTF-8"))
//        return ResponseEntity.ok()
//            .contentType(MediaType.parseMediaType("application/octet-stream"))
//            .header(HttpHeaders.CONTENT_DISPOSITION, again)
//            .body(resource)
//    }

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