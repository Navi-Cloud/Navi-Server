package com.navi.server.service

import com.navi.server.domain.FileObject
import com.navi.server.domain.GridFSRepository
import com.navi.server.domain.user.User
import com.navi.server.domain.user.UserTemplateRepository
import com.navi.server.dto.LoginRequest
import com.navi.server.dto.UserRegisterRequest
import com.navi.server.error.exception.ConflictException
import com.navi.server.error.exception.NotFoundException
import com.navi.server.security.JWTTokenProvider
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
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Path
import java.nio.file.Paths

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
}