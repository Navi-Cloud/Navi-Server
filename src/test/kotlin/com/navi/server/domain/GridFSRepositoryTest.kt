package com.navi.server.domain

import com.navi.server.domain.user.UserTemplateRepository
import com.navi.server.dto.LoginRequest
import com.navi.server.dto.UserRegisterRequest
import com.navi.server.service.FileService
import com.navi.server.service.UserService
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
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest
class GridFSRepositoryTest {
    @Autowired
    private lateinit var gridFSRepository: GridFSRepository

    @Autowired
    private lateinit var fileService: FileService

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var userTemplateRepository: UserTemplateRepository

    @Autowired
    private lateinit var gridFsTemplate: GridFsTemplate

    private val userRegisterRequest: UserRegisterRequest = UserRegisterRequest(
        userId = "kangDroid",
        userName = "KangDroid",
        userPassword = "testingPassword",
        userEmail = "test@test.com"
    )

    private fun createMultipartFile(): MockMultipartFile {
        val uploadFileName: String = "uploadTest-service.txt"
        val uploadFileContent: ByteArray = "file upload test file!".toByteArray()
        return MockMultipartFile(
            uploadFileName, uploadFileName, "text/plain", uploadFileContent
        )
    }

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
    fun is_getMetadataSpecific_works_well_same_subfolder() {
        val loginToken: String = registerUser()
        val rootToken: String = fileService.findRootToken(loginToken).rootToken
        // 'A' Subfolder
        var fileObject: FileObject = fileService.createNewFolder(loginToken, rootToken, "a")
        fileObject = fileService.createNewFolder(loginToken, fileObject.token, "a")
        val aFileObject: FileObject = fileService.fileUpload(loginToken, fileObject.token, createMultipartFile())

        // 'B' Subfolder
        fileObject = fileService.createNewFolder(loginToken, rootToken, "b")
        fileObject = fileService.createNewFolder(loginToken, fileObject.token, "a")
        val bFileObject: FileObject = fileService.fileUpload(loginToken, fileObject.token, createMultipartFile())

        assertThat(aFileObject.prevToken).isNotEqualTo(bFileObject.prevToken)
        assertThat(aFileObject.token).isNotEqualTo(bFileObject.token)

        runCatching {
            gridFSRepository.getMetadataSpecific(userRegisterRequest.userId, aFileObject.token, aFileObject.prevToken)
        }.onFailure {
            println(it.stackTraceToString())
            fail("Seems like we've found multiple implementation of afile.")
        }
    }
}