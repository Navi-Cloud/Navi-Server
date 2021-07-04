package com.navi.server.service

import com.navi.server.component.FileConfigurationComponent
import com.navi.server.domain.FileObject
import com.navi.server.domain.GridFSRepository
import com.navi.server.domain.user.User
import com.navi.server.domain.user.UserTemplateRepository
import com.navi.server.dto.LoginRequest
import com.navi.server.dto.UserRegisterRequest
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
import java.io.File
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
        }
    }
}