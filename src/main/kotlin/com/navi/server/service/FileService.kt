package com.navi.server.service

import org.springframework.stereotype.Service
import java.security.MessageDigest
import javax.xml.bind.DatatypeConverter
import kotlin.math.log
import kotlin.math.pow

@Service
class FileService {
//    @Autowired
//    private lateinit var userTemplateRepository: UserTemplateRepository
//
//    fun findAllDesc(inputUserName: String): ResponseEntity<List<FileResponseDTO>> {
//        lateinit var fileList: List<FileObject>
//        runCatching {
//            fileList = userTemplateRepository.findAllFileList(inputUserName)
//        }.onFailure {
//            throw UnknownErrorException("Unknown Exception : cannot find files")
//        }
//        return ResponseEntity
//            .status(HttpStatus.OK)
//            .body(fileList.stream()
//                .map { FileResponseDTO(it) }
//                .collect(Collectors.toList()))
//    }
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
//    fun findInsideFiles(inputUserName: String, prevToken: String): ResponseEntity<List<FileResponseDTO>> {
//        runCatching {
//            //check if this token is invalid
//            findByToken(inputUserName, prevToken)
//        }.onFailure {
//            throw NotFoundException("Cannot find file by this token : $prevToken")
//        }
//
//        // Since User Name and prevToken is verified by above statement, any error from here will be
//        // Internal Server error.
//        val result: List<FileObject> = userTemplateRepository.findAllByPrevToken(inputUserName, prevToken)
//            ?: throw UnknownErrorException("Internal Server[DB] Error Occurred. Contact developer.")
//        return ResponseEntity
//            .status(HttpStatus.OK)
//            .body(result.stream()
//                .map { FileResponseDTO(it) }
//                .collect(Collectors.toList()))
//    }
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
//    fun fileUpload(userName: String, fileToken: String, files: MultipartFile): ResponseEntity<User> {
//        // find absolutePath from token
//        val fileObject: FileObject = userTemplateRepository.findByToken(userName, fileToken)
//            ?: throw NotFoundException("Cannot find file by this token : $fileToken")
//        val uploadFolderPath: String = fileObject.fileName
//
//        // upload
//        // If the destination file already exists, it will be deleted first.
//        lateinit var uploadFile: File
//        runCatching {
//            uploadFile = File(uploadFolderPath, files.originalFilename)
//            files.transferTo(uploadFile)
//        }.onFailure {
//            when (it) {
//                is IOException -> throw FileIOException("File IO Exception")
//                else -> throw UnknownErrorException("Unknown Exception : ${it.message}")
//            }
//        }
//
//        // upload to DB
//        val basicFileAttribute: BasicFileAttributes =
//            Files.readAttributes(uploadFile.toPath(), BasicFileAttributes::class.java)
//        val simpleDateFormat: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd-HH:mm:ss")
//
//        val fileSaveRequestDTO = FileSaveRequestDTO(
//            fileName = uploadFile.absolutePath,
//            // Since we are not handling folder[recursive] upload/download, its type must be somewhat non-folder
//            fileType = "File",
//            mimeType =
//            try {
//                tika.detect(uploadFile)
//            } catch (e: Exception) {
//                println("Failed to detect mimeType for: ${e.message}")
//                "File"
//            },
//            token = getSHA256(uploadFile.absolutePath),
//            prevToken = getSHA256(uploadFolderPath),
//            lastModifiedTime = uploadFile.lastModified(),
//            fileCreatedDate = simpleDateFormat.format(basicFileAttribute.creationTime().toMillis()),
//            fileSize = convertSize(basicFileAttribute.size())
//        )
//        return save(userName, fileSaveRequestDTO)
//    }
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