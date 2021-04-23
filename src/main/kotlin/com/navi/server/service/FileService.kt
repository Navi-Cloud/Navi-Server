package com.navi.server.service

import com.navi.server.component.FileConfigurationComponent
import com.navi.server.component.FilePathResolver
import com.navi.server.domain.FileObject
import com.navi.server.domain.user.User
import com.navi.server.domain.user.UserTemplateRepository
import com.navi.server.dto.FileResponseDTO
import com.navi.server.error.exception.FileIOException
import com.navi.server.error.exception.NotFoundException
import com.navi.server.error.exception.UnknownErrorException
import com.navi.server.security.JWTTokenProvider
import org.apache.tika.Tika
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.net.URLEncoder
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.stream.Collectors
import javax.imageio.ImageIO
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

    @Autowired
    private lateinit var filePathResolver: FilePathResolver

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

    fun fileUpload(userToken: String, uploadFolderToken: String, files: MultipartFile): ResponseEntity<FileObject> {
        val userName:String = convertTokenToUserName(userToken)

        // find absolutePath from token
        val fileObject: FileObject = userTemplateRepository.findByToken(userName, uploadFolderToken)

        // Need to Con-cat string to real path
        val uploadFolderPath: String = filePathResolver.convertFileNameToFullPath(userName, fileObject.fileName)

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

        // Windows Implementation
        val dbTargetFilename: String = filePathResolver.convertPhysicsPathToServerPath(uploadFile.absolutePath, userName)

        // Get Prev Token
        val prevTokenString: String = filePathResolver.convertPhysicsPathToPrevServerPath(uploadFile.absolutePath, userName)

        val saveFileObject: FileObject = FileObject(
            fileName = dbTargetFilename,
            // Since we are not handling folder[recursive] upload/download, its type must be somewhat non-folder
            fileType = "File",
            mimeType =
            try {
                tika.detect(uploadFile)
            } catch (e: Exception) {
                println("Failed to detect mimeType for: ${e.message}")
                "File"
            },
            token = getSHA256(dbTargetFilename),
            prevToken = getSHA256(prevTokenString),
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

    fun getFileObjectByUserName(userName: String, fileToken: String): FileObject {
        return runCatching {
            userTemplateRepository.findByToken(userName, fileToken)
        }.getOrThrow()
    }

    fun fileDownload(userToken: String, fileToken: String): ResponseEntity<StreamingResponseBody> {
        val inputUserName: String = convertTokenToUserName(userToken)

        val file: FileObject = getFileObjectByUserName(inputUserName, fileToken)
        val realFilePath: Path = Paths.get(
            fileConfigurationComponent.serverRoot,
            inputUserName,
            file.fileName
        )

        if (!Files.exists(realFilePath)) {
            throw NotFoundException("Cannot find file: $realFilePath")
        }

        val responseBody = StreamingResponseBody { outputStream: OutputStream? -> Files.copy(realFilePath, outputStream) }

        val fileName: String = file.fileName
            .replace("\\", "/")
            .split("/")
            .last()
        val again: String =
            String.format("attachment; filename=\"%s\"", URLEncoder.encode(fileName, "UTF-8"))
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/octet-stream"))
            .header(HttpHeaders.CONTENT_DISPOSITION, again)
            .body(responseBody)
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