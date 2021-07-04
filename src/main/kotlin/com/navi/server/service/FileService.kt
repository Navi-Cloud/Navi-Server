package com.navi.server.service

import com.navi.server.domain.FileObject
import com.navi.server.domain.GridFSRepository
import com.navi.server.dto.RootTokenResponseDto
import com.navi.server.error.exception.NotFoundException
import com.navi.server.security.JWTTokenProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.io.InputStream
import java.io.OutputStream
import java.net.URLEncoder
import java.security.MessageDigest
import javax.xml.bind.DatatypeConverter
import kotlin.math.log
import kotlin.math.pow

@Service
class FileService {
    @Autowired
    private lateinit var jwtTokenProvider: JWTTokenProvider

    @Autowired
    private lateinit var gridFSRepository: GridFSRepository

    private fun convertTokenToUserId(inputToken: String): String {
        var userId: String = ""
        runCatching {
            jwtTokenProvider.getUserPk(inputToken)
        }.onSuccess {
            userId = it
        }.onFailure {
            throw NotFoundException("Userid is NOT Found!")
        }

        return userId
    }

    fun findRootToken(userToken: String): RootTokenResponseDto {
        val userId: String = convertTokenToUserId(userToken)
        val userFileObject: FileObject = gridFSRepository.getRootToken(userId, getSHA256("/"))

        return RootTokenResponseDto(userFileObject.token)
    }

    fun findInsideFiles(userToken: String, prevToken: String): List<FileObject> {
        val userId: String = convertTokenToUserId(userToken)

        // Check if token is actually exists!
        return gridFSRepository.getMetadataInsideFolder(
            userId = userId,
            targetPrevToken = prevToken
        )
    }

    fun fileUpload(userToken: String, uploadFolderToken: String, files: MultipartFile): FileObject {
        val userId:String = convertTokenToUserId(userToken)
        val tmpFileObject: FileObject = FileObject(
            userId = userId,
            token = getSHA256(files.originalFilename),
            prevToken = uploadFolderToken,
            fileName = files.originalFilename,
            fileType = "File"
        )

        // Save!
        gridFSRepository.saveToGridFS(
            fileObject = tmpFileObject,
            inputStream = files.inputStream
        )

        return tmpFileObject
    }

    fun getFileObjectByUserId(userId: String, fileToken: String, prevToken: String): FileObject {
        return gridFSRepository.getMetadataSpecific(userId, fileToken, prevToken)
    }

    fun fileDownload(userToken: String, fileToken: String, prevToken: String): ResponseEntity<StreamingResponseBody> {
        val inputUserId: String = convertTokenToUserId(userToken)

        // File Object[MetaData]
        val file: FileObject = getFileObjectByUserId(inputUserId, fileToken, prevToken)

        // Actual file itself[stream]
        val gridFSFile: InputStream = gridFSRepository.getFullTargetStream(inputUserId, file)

        // Streaming ResponseBody
        val responseBody = StreamingResponseBody { outputStream: OutputStream? -> gridFSFile.transferTo(outputStream) }

        val again: String =
            String.format("attachment; filename=\"%s\"", URLEncoder.encode(file.fileName, "UTF-8"))

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/octet-stream"))
            .header(HttpHeaders.CONTENT_DISPOSITION, again)
            .body(responseBody)
    }

    private fun createLogicalFolder(userId: String, prevToken: String, newFolderName: String) {
        gridFSRepository.saveToGridFS(
            fileObject = FileObject(
                userId = userId,
                fileName = newFolderName,
                fileType = "Folder",
                token = getSHA256(newFolderName),
                prevToken = prevToken
            ),
            inputStream = InputStream.nullInputStream()
        )
    }

    fun createNewFolder(userToken: String, parentFolderToken: String, newFolderName: String){
        val userId: String = convertTokenToUserId(userToken)

        // Step 2) upload to DB
        createLogicalFolder(userId, parentFolderToken, newFolderName)
    }

    fun createRootUser(userId: String) {
        gridFSRepository.saveToGridFS(
            fileObject = FileObject(
                userId = userId,
                fileName = "/",
                fileType = "Folder",
                token = getSHA256("/"),
                prevToken = ""
            ),
            inputStream = InputStream.nullInputStream()
        )
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