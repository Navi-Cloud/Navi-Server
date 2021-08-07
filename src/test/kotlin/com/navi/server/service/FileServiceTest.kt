package com.navi.server.service

import com.navi.server.domain.FileObject
import com.navi.server.domain.GridFSRepository
import com.navi.server.domain.user.UserTemplateRepository
import com.navi.server.dto.LoginRequest
import com.navi.server.dto.UserRegisterRequest
import com.navi.server.error.exception.ConflictException
import com.navi.server.error.exception.NotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.gridfs.GridFsTemplate
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.io.ByteArrayOutputStream

@RunWith(SpringRunner::class)
@SpringBootTest
class FileServiceTest {
    @Autowired
    private lateinit var fileService: FileService

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var userTemplateRepository: UserTemplateRepository

    @Autowired
    private lateinit var gridFsTemplate: GridFsTemplate

    @Autowired
    private lateinit var gridFSRepository: GridFSRepository

    private val userRegisterRequest: UserRegisterRequest = UserRegisterRequest(
        userId = "kangDroid",
        userName = "KangDroid",
        userPassword = "testingPassword",
        userEmail = "test@test.com"
    )

    @After
    @Before
    fun clearAll() {
        userTemplateRepository.clearAll()
        gridFsTemplate.delete(Query())
    }

    fun registerUser(): String {
        userService.registerUser(userRegisterRequest)

        return userService.loginUser(
            LoginRequest(
                userId = userRegisterRequest.userId,
                userPassword = "testingPassword"
            )
        ).body!!.userToken
    }

    @Test
    fun is_findRootToken_works_well() {
        val userToken: String = registerUser()

        fileService.findRootToken(userToken).also {
            assertThat(it.rootToken).isNotEqualTo("")
        }
    }

    @Test
    fun is_findRootToken_fails_wrong_token() {
        runCatching {
            fileService.findRootToken("")
        }.onSuccess {
            fail("No token but succeed!")
        }.onFailure {
            assertThat(it is NotFoundException).isEqualTo(true)
        }
    }

    @Test
    fun is_findInsideFiles_works_well() {
        val userToken: String = registerUser()
        val rootToken: String = fileService.findRootToken(userToken).rootToken

        fileService.findInsideFiles(userToken, rootToken).also {
            assertThat(it.isEmpty()).isEqualTo(true)
        }
    }

    @Test
    fun is_fileUpload_works_well() {
        val userToken: String = registerUser()
        val rootToken: String = fileService.findRootToken(userToken).rootToken

        // make uploadFile
        val uploadFileName: String = "uploadTest-service.txt"
        val uploadFileContent: ByteArray = "file upload test file!".toByteArray()
        val multipartFile: MockMultipartFile = MockMultipartFile(
            uploadFileName, uploadFileName, "text/plain", uploadFileContent
        )

        val responseFileObject: FileObject = fileService.fileUpload(
            userToken = userToken,
            uploadFolderToken = rootToken,
            files = multipartFile
        )

        // Check Response
        with (responseFileObject) {
            assertThat(fileName).isEqualTo(uploadFileName)
            assertThat(prevToken).isEqualTo(rootToken)
        }

        // Check DB
        gridFSRepository.getMetadataInsideFolder(userRegisterRequest.userId, responseFileObject.prevToken).also {
            assertThat(it.isEmpty()).isEqualTo(false)
            assertThat(it.size).isEqualTo(1)
            assertThat(it[0].fileName).isEqualTo(uploadFileName)
        }
    }

    @Test
    fun is_fileUpload_conflict_duplicated_file() {
        val userToken: String = registerUser()
        val rootToken: String = fileService.findRootToken(userToken).rootToken

        // make uploadFile
        val uploadFileName: String = "uploadTest-service.txt"
        val uploadFileContent: ByteArray = "file upload test file!".toByteArray()
        val multipartFile: MockMultipartFile = MockMultipartFile(
            uploadFileName, uploadFileName, "text/plain", uploadFileContent
        )

        fileService.fileUpload(
            userToken = userToken,
            uploadFolderToken = rootToken,
            files = multipartFile
        )

        runCatching {
            fileService.fileUpload(
                userToken = userToken,
                uploadFolderToken = rootToken,
                files = multipartFile
            )
        }.onSuccess {
            fail("File is already uploaded but succeed?")
        }.onFailure {
            assertThat(it is ConflictException).isEqualTo(true)
        }
    }

    @Test
    fun is_fileDownload_works_well() {
        val userToken: String = registerUser()

        // First we upload files first to root
        val rootToken: String = fileService.findRootToken(userToken).rootToken

        // make uploadFile
        val uploadFileName: String = "uploadTest-service.txt"
        val uploadFileContent: ByteArray = "file upload test file!".toByteArray()
        val multipartFile: MockMultipartFile = MockMultipartFile(
            uploadFileName, uploadFileName, "text/plain", uploadFileContent
        )
        val fileObjectUploaded: FileObject = fileService.fileUpload(
            userToken = userToken,
            uploadFolderToken = rootToken,
            files = multipartFile
        )

        // Now Download
        val byteArrayOutputStream: ByteArrayOutputStream = ByteArrayOutputStream()
        fileService.fileDownload(
            userToken = userToken,
            fileToken = fileObjectUploaded.token,
            prevToken = fileObjectUploaded.prevToken
        ).also {
            assertThat(it.statusCode).isEqualTo(HttpStatus.OK)
            val streamBody: StreamingResponseBody = it.body!!
            streamBody.writeTo(byteArrayOutputStream)
        }

        // Compare results
        assertThat(byteArrayOutputStream.toByteArray()).isEqualTo(uploadFileContent)
    }

    @Test
    fun is_fileDownload_non_exists_not_found() {
        val userToken: String = registerUser()

        // First we upload files first to root
        val rootToken: String = fileService.findRootToken(userToken).rootToken

        runCatching {
            fileService.fileDownload(userToken, "", rootToken)
        }.onSuccess {
            fail("Since we are not providing fileToken, but it succeed?")
        }.onFailure {
            assertThat(it is NotFoundException).isEqualTo(true)
        }
    }


    @Test
    fun is_createNewFolder_works_well() {
        val userToken: String = registerUser()
        val rootToken: String = fileService.findRootToken(userToken).rootToken

        // Create it
        val fileObject: FileObject = fileService.createNewFolder(userToken, rootToken, "Testing")

        // Check
        gridFSRepository.getMetadataSpecific(userRegisterRequest.userId, fileObject.token, rootToken).also {
            assertThat(it.fileName).isEqualTo("Testing")
            assertThat(it.fileType).isEqualTo("Folder")
            assertThat(it.prevToken).isEqualTo(rootToken)
        }
    }

    @Test
    fun is_createNewFolder_duplicated_conflict() {
        val userToken: String = registerUser()
        val rootToken: String = fileService.findRootToken(userToken).rootToken

        // Create it
        fileService.createNewFolder(userToken, rootToken, "Testing")

        // Check
        runCatching {
            fileService.createNewFolder(userToken, rootToken, "Testing")
        }.onSuccess {
            fail("We created folder, but it succeed?")
        }.onFailure {
            assertThat(it is ConflictException).isEqualTo(true)
        }
    }

    @Test
    fun is_convertSize_works_well() {
        val testFileSizeMib: Long = 1024 * 1024 * 2 // 2 Mib
        val testFileSizeKib: Long = 1024 * 4 // 4.0Kib
        val testFileSizeB: Long = 800 //800B
        val testFileSizeZero: Long = 0
        assertThat(fileService.convertSize(testFileSizeMib)).isEqualTo("2.0MiB")
        assertThat(fileService.convertSize(testFileSizeKib)).isEqualTo("4.0KiB")
        assertThat(fileService.convertSize(testFileSizeB)).isEqualTo("800B")
        assertThat(fileService.convertSize(testFileSizeZero)).isEqualTo("0B")
    }

    @Test
    fun is_removeFile_with_file_works_welll() {
        // Upload first
        val userToken: String = registerUser()
        val rootToken: String = fileService.findRootToken(userToken).rootToken

        // make uploadFile
        val uploadFileName: String = "uploadTest-service.txt"
        val uploadFileContent: ByteArray = "file upload test file!".toByteArray()
        val multipartFile: MockMultipartFile = MockMultipartFile(
            uploadFileName, uploadFileName, "text/plain", uploadFileContent
        )

        val responseFileObject: FileObject = fileService.fileUpload(
            userToken = userToken,
            uploadFolderToken = rootToken,
            files = multipartFile
        )

        gridFSRepository.getMetadataInsideFolder(userRegisterRequest.userId, responseFileObject.prevToken).also {
            assertThat(it.size).isEqualTo(1)
        }

        // Remove
        fileService.removeFile(
            userToken = userToken,
            targetToken = responseFileObject.token,
            prevToken = responseFileObject.prevToken
        )

        // Check
        gridFSRepository.getMetadataInsideFolder(userRegisterRequest.userId, responseFileObject.prevToken).also {
            assertThat(it.size).isEqualTo(0)
        }
    }

    @Test
    fun is_removeFile_with_folder_works_well() {
        // Upload first
        val userToken: String = registerUser()
        val rootToken: String = fileService.findRootToken(userToken).rootToken

        // Create Folder
        val folderObject: FileObject = fileService.createNewFolder(userToken, rootToken, "TestingFolder")

        // make uploadFile
        val uploadFileName: String = "uploadTest-service.txt"
        val uploadFileContent: ByteArray = "file upload test file!".toByteArray()
        val multipartFile: MockMultipartFile = MockMultipartFile(
            uploadFileName, uploadFileName, "text/plain", uploadFileContent
        )

        val responseFileObject: FileObject = fileService.fileUpload(
            userToken = userToken,
            uploadFolderToken = folderObject.token,
            files = multipartFile
        )

        // Mid-Check
        assertThat(gridFsTemplate.find(Query()).count()).isEqualTo(3) // root, folder, file

        // Now delete folder
        fileService.removeFile(
            userToken = userToken,
            targetToken = folderObject.token,
            prevToken = rootToken
        )

        // Check
        assertThat(gridFsTemplate.find(Query()).count()).isEqualTo(1) // Only root
    }

    @Test
    fun is_searchFile_works_well() {
        // Upload first
        val userToken: String = registerUser()
        val rootToken: String = fileService.findRootToken(userToken).rootToken

        // make uploadFile
        val uploadFileName: String = "uploadTest-service.txt"
        val uploadFileContent: ByteArray = "file upload test file!".toByteArray()
        val multipartFile: MockMultipartFile = MockMultipartFile(
            uploadFileName, uploadFileName, "text/plain", uploadFileContent
        )

        val responseFileObject: FileObject = fileService.fileUpload(
            userToken = userToken,
            uploadFolderToken = rootToken,
            files = multipartFile
        )

        // Search
        fileService.searchFile(userToken, responseFileObject.fileName).also {
            assertThat(it.size).isEqualTo(1)
            assertThat(it[0].fileType).isEqualTo(responseFileObject.fileType)
            assertThat(it[0].fileName).isEqualTo(responseFileObject.fileName)
        }
    }

    @Test
    fun is_searchFile_works_well_when_partial_match() {
        // Upload first
        val userToken: String = registerUser()
        val rootToken: String = fileService.findRootToken(userToken).rootToken

        // make uploadFile
        val uploadFileName: String = "uploadTest-service.txt"
        val uploadFileContent: ByteArray = "file upload test file!".toByteArray()
        val multipartFile: MockMultipartFile = MockMultipartFile(
            uploadFileName, uploadFileName, "text/plain", uploadFileContent
        )

        val responseFileObject: FileObject = fileService.fileUpload(
            userToken = userToken,
            uploadFolderToken = rootToken,
            files = multipartFile
        )

        // Search with substring (partial match)
        fileService.searchFile(userToken, responseFileObject.fileName.substring(2, 5)).also {
            assertThat(it.size).isEqualTo(1)
            assertThat(it[0].fileType).isEqualTo(responseFileObject.fileType)
            assertThat(it[0].fileName).isEqualTo(responseFileObject.fileName)
        }
    }

    @Test
    fun is_findFolderFromToken_works_well() {
        // Create Folder first
        val userToken: String = registerUser()
        val rootToken: String = fileService.findRootToken(userToken).rootToken

        val fileObject: FileObject = fileService.createNewFolder(userToken, rootToken, "Testing")

        // Find Folder From Token and Assert it
        fileService.findFolderFromToken(userToken, fileObject.token).also {
            assertThat(it).isEqualToComparingFieldByField(fileObject)
        }
    }

    @Test
    fun is_copyFile_works_well() {
        // Upload first
        val userToken: String = registerUser()
        val rootToken: String = fileService.findRootToken(userToken).rootToken

        // make uploadFile
        val uploadFileName: String = "uploadTest-service.txt"
        val uploadFileContent: ByteArray = "file upload test file!".toByteArray()
        val multipartFile: MockMultipartFile = MockMultipartFile(
            uploadFileName, uploadFileName, "text/plain", uploadFileContent
        )

        val responseFileObject: FileObject = fileService.fileUpload(
            userToken = userToken,
            uploadFolderToken = rootToken,
            files = multipartFile
        )

        // Copy it
        fileService.migrateFile(userToken, responseFileObject.token, responseFileObject.prevToken, rootToken, "newFileName", true)

        // Check it
        val rootList: List<FileObject> = gridFSRepository.getMetadataInsideFolder(userRegisterRequest.userId, rootToken)
        assertThat(rootList.size).isEqualTo(2)

        (rootList.find { it.fileName == "newFileName"} ?: fail("copied file should not be null!")).also {
            assertThat(it.fileName).isEqualTo("newFileName")
        }
    }

    @Test
    fun is_migrateFile_works_well_move() {
        // Upload first
        val userToken: String = registerUser()
        val rootToken: String = fileService.findRootToken(userToken).rootToken

        // make uploadFile
        val uploadFileName: String = "uploadTest-service.txt"
        val uploadFileContent: ByteArray = "file upload test file!".toByteArray()
        val multipartFile: MockMultipartFile = MockMultipartFile(
            uploadFileName, uploadFileName, "text/plain", uploadFileContent
        )

        val responseFileObject: FileObject = fileService.fileUpload(
            userToken = userToken,
            uploadFolderToken = rootToken,
            files = multipartFile
        )

        // Copy it
        fileService.migrateFile(userToken, responseFileObject.token, responseFileObject.prevToken, rootToken, "newFileName", false)

        // Check it
        val rootList: List<FileObject> = gridFSRepository.getMetadataInsideFolder(userRegisterRequest.userId, rootToken)
        assertThat(rootList.size).isEqualTo(1)

        (rootList.find { it.fileName == "newFileName"} ?: fail("copied file should not be null!")).also {
            assertThat(it.fileName).isEqualTo("newFileName")
        }
    }

    @Test
    fun is_copyFolder_works_well_with_folders() {
        // Upload first
        val userToken: String = registerUser()
        val rootToken: String = fileService.findRootToken(userToken).rootToken

        val targetFolder: FileObject = fileService.createNewFolder(userToken, rootToken, "Target")
        val toFolder: FileObject = fileService.createNewFolder(userToken, rootToken, "To")

        // Make Folders for copy
        fileService.createNewFolder(userToken, targetFolder.token, "FoldERA")
        fileService.createNewFolder(userToken, targetFolder.token, "FoldERB")

        // Perform: Copy it
        fileService.copyFolder(userToken, targetFolder.prevToken, targetFolder.token, toFolder.token)

        // Assert
        val fromFolderFiles: List<FileObject> = gridFSRepository.getMetadataInsideFolder(userRegisterRequest.userId, targetFolder.token)
        val copiedFolder: FileObject = gridFSRepository.getMetadataInsideFolder(userRegisterRequest.userId, toFolder.token)
            .find { it.fileName == targetFolder.fileName} ?: fail("This Should be succeed")
        val copiedFolderFiles: List<FileObject> =  gridFSRepository.getMetadataInsideFolder(userRegisterRequest.userId, copiedFolder.token)

        assertThat(copiedFolderFiles.size).isEqualTo(fromFolderFiles.size)
        copiedFolderFiles.filter { it.fileType == "Folder" && it.fileName.contains("FoldER") }.also {
            assertThat(it.size).isEqualTo(2)
        }
    }

    private fun upload_text_file_for_test(userToken: String, folderToken: String, fileName: String, fileContent: String): FileObject{
        // make uploadFile and upload
        val uploadFileContent: ByteArray = fileContent.toByteArray()
        val multipartFile: MockMultipartFile = MockMultipartFile(
            fileName, fileName, "text/plain", uploadFileContent
        )
        return fileService.fileUpload(userToken, folderToken, multipartFile)
    }

    @Test
    fun is_copyFolder_works_well_with_files(){
        // Upload first
        val userToken: String = registerUser()
        val rootToken: String = fileService.findRootToken(userToken).rootToken

        val targetFolder: FileObject = fileService.createNewFolder(userToken, rootToken, "Target")
        val toFolder: FileObject = fileService.createNewFolder(userToken, rootToken, "To")

        // Make Files for copy
        //fileService.createNewFolder(userToken, targetFolder.token, "TesTA")
        upload_text_file_for_test(userToken, targetFolder.token, "FilE1", "test")
        upload_text_file_for_test(userToken, targetFolder.token, "FilE2", "test")

        // Perform: Copy it
        fileService.copyFolder(userToken, targetFolder.prevToken, targetFolder.token, toFolder.token)

        // Assert
        val fromFolderFiles: List<FileObject> = gridFSRepository.getMetadataInsideFolder(userRegisterRequest.userId, targetFolder.token)
        val copiedFolder: FileObject = gridFSRepository.getMetadataInsideFolder(userRegisterRequest.userId, toFolder.token)
            .find { it.fileName == targetFolder.fileName} ?: fail("This Should be succeed")
        val copiedFolderFiles: List<FileObject> =  gridFSRepository.getMetadataInsideFolder(userRegisterRequest.userId, copiedFolder.token)

        assertThat(copiedFolderFiles.size).isEqualTo(fromFolderFiles.size)
        copiedFolderFiles.filter { it.fileType == "File" && it.fileName.contains("FilE") }.also {
            assertThat(it.size).isEqualTo(2)
        }
    }

    @Test
    fun is_copyFolder_works_well_with_folders_and_files(){
        // Upload first
        val userToken: String = registerUser()
        val rootToken: String = fileService.findRootToken(userToken).rootToken

        val targetFolder: FileObject = fileService.createNewFolder(userToken, rootToken, "Target")
        val toFolder: FileObject = fileService.createNewFolder(userToken, rootToken, "To")

        // Make folder and files for copy
        // : Make 1 files under targetFolder
        upload_text_file_for_test(userToken, targetFolder.token, "Target_FilE1", "test")
        // : Make 1 child folder and 2 files under it
        val childFolder1: FileObject = fileService.createNewFolder(userToken, targetFolder.token, "Target_FoldER_A")
        upload_text_file_for_test(userToken, childFolder1.token, "FilE_AA1", "test")
        fileService.createNewFolder(userToken, childFolder1.token, "FoldER_AA")

        // Perform: Copy it
        fileService.copyFolder(userToken, targetFolder.prevToken, targetFolder.token, toFolder.token)

        // Assert
        val fromFolderFiles: List<FileObject> = gridFSRepository.getMetadataInsideFolder(userRegisterRequest.userId, targetFolder.token)
        val copiedFolder: FileObject = gridFSRepository.getMetadataInsideFolder(userRegisterRequest.userId, toFolder.token)
            .find { it.fileName == targetFolder.fileName} ?: fail("This Should be succeed")
        val copiedFolderFiles: List<FileObject> =  gridFSRepository.getMetadataInsideFolder(userRegisterRequest.userId, copiedFolder.token)

        assertThat(copiedFolderFiles.size).isEqualTo(fromFolderFiles.size)
        copiedFolderFiles.filter { it.fileName.contains("Target") }.also {
            assertThat(it.size).isEqualTo(2)
        }

        // Check Child Folder
        val copiedChildFolder: FileObject = copiedFolderFiles.find { it.fileType == "Folder" && it.fileName.contains("A")} ?: fail("This Should be succeed")
        val copiedChildFolderFiles: List<FileObject> =  gridFSRepository.getMetadataInsideFolder(userRegisterRequest.userId, copiedChildFolder.token)
        assertThat(copiedChildFolderFiles.size).isEqualTo(2)
        copiedChildFolderFiles.filter { it.fileName.contains("AA") }.also {
            assertThat(it.size).isEqualTo(2)
        }
    }

    @Test
    fun is_copyFolder_works_well_with_empty_folder() {
        // Upload first
        val userToken: String = registerUser()
        val rootToken: String = fileService.findRootToken(userToken).rootToken

        val targetFolder: FileObject = fileService.createNewFolder(userToken, rootToken, "Target")
        val toFolder: FileObject = fileService.createNewFolder(userToken, rootToken, "To")

        // Perform: Copy it
        fileService.copyFolder(userToken, targetFolder.prevToken, targetFolder.token, toFolder.token)

        // Assert
        val fromFolderFiles: List<FileObject> = gridFSRepository.getMetadataInsideFolder(userRegisterRequest.userId, targetFolder.token)
        val copiedFolder: FileObject = gridFSRepository.getMetadataInsideFolder(userRegisterRequest.userId, toFolder.token)
            .find { it.fileName == targetFolder.fileName} ?: fail("This Should be succeed")
        val copiedFolderFiles: List<FileObject> =  gridFSRepository.getMetadataInsideFolder(userRegisterRequest.userId, copiedFolder.token)

        assertThat(copiedFolderFiles.size).isEqualTo(fromFolderFiles.size)
    }

    @Test
    fun is_copyFolder_throw_NotFoundException() {
        // copyFolder throw NotFoundException when request with "File", not "Folder"

        // Upload first
        val userToken: String = registerUser()
        val rootToken: String = fileService.findRootToken(userToken).rootToken

        val toFolder: FileObject = fileService.createNewFolder(userToken, rootToken, "To")

        // Make file: invalid request for copyFolder
        val invalidRequestFile: FileObject = upload_text_file_for_test(userToken, rootToken, "FilE1", "test")

        // Perform: Copy it
        runCatching {
            fileService.copyFolder(userToken, invalidRequestFile.prevToken, invalidRequestFile.token, toFolder.token)
        }.onSuccess {
            fail("This should be failed...")
        }.onFailure {
            assertThat(it is NotFoundException).isEqualTo(true)
        }
    }

    @Test
    fun is_copyFolder_throw_ConflictException() {
        // copyFolder throw ConflictException when request folder already exists in toFolder

        // Set
        val userToken: String = registerUser()
        val rootToken: String = fileService.findRootToken(userToken).rootToken

        val targetFolder: FileObject = fileService.createNewFolder(userToken, rootToken, "Target")
        val toFolder: FileObject = fileService.createNewFolder(userToken, rootToken, "To")

        // Create folder in toFolder with the same name as targetFolder
        fileService.createNewFolder(userToken, toFolder.token, targetFolder.fileName)

        // Perform: Copy it
        runCatching {
            fileService.copyFolder(userToken, targetFolder.prevToken, targetFolder.token, toFolder.token)
        }.onSuccess {
            fail("This should be failed...")
        }.onFailure {
            assertThat(it is ConflictException).isEqualTo(true)
        }
    }
}