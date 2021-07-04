package com.navi.server.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.navi.server.component.FileConfigurationComponent
import com.navi.server.domain.FileObject
import com.navi.server.domain.GridFSRepository
import com.navi.server.domain.user.User
import com.navi.server.domain.user.UserTemplateRepository
import com.navi.server.dto.*
import org.springframework.boot.test.web.client.exchange
import com.navi.server.security.JWTTokenProvider
import com.navi.server.service.FileService
import com.navi.server.service.UserService
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.test.context.junit4.SpringRunner
import org.assertj.core.api.Assertions.assertThat;
import org.junit.Before
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.gridfs.GridFsTemplate
import org.springframework.http.*
import org.springframework.http.ResponseEntity.status
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.io.File
import org.springframework.web.context.WebApplicationContext
import java.nio.file.Path
import java.nio.file.Paths

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FileApiControllerTest {
    @LocalServerPort
    var port: Int = 0

    private val restTemplate: TestRestTemplate = TestRestTemplate()

    @Autowired
    private lateinit var fileService: FileService

    @Autowired
    private lateinit var userTemplateRepository: UserTemplateRepository

    @Autowired
    private lateinit var fileConfigurationComponent: FileConfigurationComponent

    @Autowired
    private lateinit var jwtTokenProvider: JWTTokenProvider

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private lateinit var mockMvc : MockMvc

    @Autowired
    private lateinit var gridFsTemplate: GridFsTemplate

    @Autowired
    private lateinit var  gridFSRepository: GridFSRepository

    private val mockUser: UserRegisterRequest = UserRegisterRequest(
        userId = "kangDroid",
        userName = "Kangdroid",
        userPassword = "password",
        userEmail = "test@test.com"
    )

    @Before
    fun initEnvironment() {
        gridFsTemplate.delete(Query())
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
    }

    @After
    fun destroyEnvironment() {
        gridFsTemplate.delete(Query())
        userTemplateRepository.clearAll()
    }

    private fun registerAndLogin(): String {

        userService.registerUser(
            mockUser
        )

        val loginResponseDto: ResponseEntity<LoginResponse> = userService.loginUser(
                LoginRequest(
                    userId = mockUser.userId,
                    userPassword = mockUser.userPassword
                )
        )

        return loginResponseDto.body.userToken
    }

    @Test
    fun testFindRootToken() {
        val loginToken: String = registerAndLogin()

        // Get Api
        val url = "http://localhost:$port/api/navi/root-token"
        val headers: HttpHeaders = HttpHeaders().apply {
            add("X-AUTH-TOKEN", loginToken)
        }

        val responseEntity : ResponseEntity<RootTokenResponseDto> = restTemplate.exchange(url, HttpMethod.GET, HttpEntity<Void>(headers))

        // Assert
        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(responseEntity.body.rootToken).isNotEqualTo("")
    }

    @Test
    fun testFindInsideFiles_ok() {
        val loginToken: String = registerAndLogin()
        val rootToken: String = fileService.findRootToken(loginToken).rootToken

        // in "folderName" folder
        val url2 = "http://localhost:$port/api/navi/files/list/${rootToken}"
        val headers: HttpHeaders = HttpHeaders().apply {
            add("X-AUTH-TOKEN", loginToken)
        }
        restTemplate.exchange(
            url2, HttpMethod.GET, HttpEntity<Void>(headers), Array<FileObject>::class.java
        ).also {
            assertThat(it.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(it.body!!.size).isEqualTo(0L) // Empty at first.
        }
    }

    // test upload
    @Test
    fun testFileUpload_ok(){
        // Create Server Root Structure
        val loginToken: String = registerAndLogin()
        val rootToken: String = fileService.findRootToken(loginToken).rootToken

        // make uploadFile
        val uploadFileName: String = "uploadTest-service.txt"
        val uploadFileContent: ByteArray = "file upload test file!".toByteArray()
        val multipartFile: MockMultipartFile = MockMultipartFile(
            "uploadFile", uploadFileName, "text/plain", uploadFileContent
        )

        // file upload
        val uploadFolderPath = MockMultipartFile("uploadPath", "uploadPath", "text/plain", rootToken.toByteArray())

        // Perform
        mockMvc.perform(
            MockMvcRequestBuilders.multipart("/api/navi/files")
                .file(multipartFile)
                .file(uploadFolderPath)
                .header("X-AUTH-TOKEN", loginToken)
        ).andExpect { status(HttpStatus.OK) }
            .andDo(MockMvcResultHandlers.print())
            .andDo{
                assertThat(it.response.status).isEqualTo(HttpStatus.OK.value())
            }

        // Now Check DB
        gridFSRepository.getMetadataInsideFolder("kangDroid", rootToken).also {
            assertThat(it.isEmpty()).isEqualTo(false)
            assertThat(it.size).isEqualTo(1)
            assertThat(it[0].fileName).isEqualTo(uploadFileName)
        }
    }

    @Test
    fun testFileUpload_quotationMarkedFileName_ok(){
        // Create Server Root Structure
        val loginToken: String = registerAndLogin()
        val rootToken: String = fileService.findRootToken(loginToken).rootToken

        // make uploadFile
        val uploadFileName: String = "uploadTest-service.txt"
        val uploadFileContent: ByteArray = "file upload test file!".toByteArray()
        val multipartFile: MockMultipartFile = MockMultipartFile(
            "uploadFile", uploadFileName, "text/plain", uploadFileContent
        )

        // file upload
        // insert quotation mark
        val uploadFolderToken = "\"" + rootToken + "\""  // Upload to user's root
        val uploadFolderPath = MockMultipartFile("uploadPath", "uploadPath", "text/plain", uploadFolderToken.toByteArray())

        // Perform
        mockMvc.perform(
            MockMvcRequestBuilders.multipart("/api/navi/files")
                .file(multipartFile)
                .file(uploadFolderPath)
                .header("X-AUTH-TOKEN", loginToken)
        ).andExpect { status(HttpStatus.OK) }
            .andDo(MockMvcResultHandlers.print())
            .andDo{
                assertThat(it.response.status).isEqualTo(HttpStatus.OK.value())
            }

        // Assert
        // Whether uploaded text file is actually exists
        gridFSRepository.getMetadataInsideFolder("kangDroid", rootToken).also {
            assertThat(it.isEmpty()).isEqualTo(false)
            assertThat(it.size).isEqualTo(1)
            assertThat(it[0].fileName).isEqualTo(uploadFileName)
        }
    }

    // test fileDownload
//    @Test
//    fun testFileDownload_ok(){
//        // Let
//        val fileObject: FileObject = FileObject(
//            fileName = "test_file",
//            fileType = "test_type",
//            mimeType = "mime_type",
//            token = "test_token",
//            prevToken = "prev_token",
//            lastModifiedTime = System.currentTimeMillis(),
//            fileCreatedDate = "date",
//            fileSize = "5m"
//        )
//
//        val loginToken: String = registerAndLogin()
//        val user: User = userTemplateRepository.findByUserId("kangDroid")
//        user.fileList.add(fileObject)
//        userTemplateRepository.save(user)
//        // Write some texts
//        Paths.get(fileConfigurationComponent.serverRoot, "kangDroid").toFile().mkdirs()
//        val file: File = Paths.get(fileConfigurationComponent.serverRoot, "kangDroid", fileObject.fileName).toFile()
//        file.writeText("Test!")
//
//
//        // Perform and Assert
//        mockMvc.perform(
//            MockMvcRequestBuilders.get("/api/navi/files/${fileObject.token}")
//                .header("X-AUTH-TOKEN", loginToken)
//        ).andExpect { status(HttpStatus.OK) }
//            .andDo(MockMvcResultHandlers.print())
//            .andDo{
//                assertThat(it.response.status).isEqualTo(HttpStatus.OK.value())
//            }
//    }
//
//    @Test
//    fun invalid_FileDownload_NotFound() {
//        val loginToken: String = registerAndLogin()
//
//        // Let
//        val fileObject: FileObject = FileObject(
//            fileName = "test_file",
//            fileType = "test_type",
//            mimeType = "mime_type",
//            token = "test_token",
//            prevToken = "prev_token",
//            lastModifiedTime = System.currentTimeMillis(),
//            fileCreatedDate = "date",
//            fileSize = "5m"
//        )
//
//        // invalid file Download 1 : invalid token
//        val url = "http://localhost:$port/api/navi/files/${fileObject.token}"
//        mockMvc.perform(
//            MockMvcRequestBuilders.get("/api/navi/files/${fileObject.token}")
//                .header("X-AUTH-TOKEN", loginToken)
//        ).andExpect { status(HttpStatus.NOT_FOUND) }
//            .andDo(MockMvcResultHandlers.print())
//            .andDo{
//                assertThat(it.response.status).isEqualTo(HttpStatus.NOT_FOUND.value())
//            }
//
//        // invalid file Download 2 : FileNotFoundException (no such file at server)
//        // save to (only) DB
//        val user: User = userTemplateRepository.findByUserId("kangDroid")
//        user.fileList.add(fileObject)
//        userTemplateRepository.save(user)
//
//        mockMvc.perform(
//            MockMvcRequestBuilders.get("/api/navi/files/${fileObject.token}")
//                .header("X-AUTH-TOKEN", loginToken)
//        ).andExpect { status(HttpStatus.NOT_FOUND) }
//            .andDo(MockMvcResultHandlers.print())
//            .andDo{
//                assertThat(it.response.status).isEqualTo(HttpStatus.NOT_FOUND.value())
//            }
//    }
//
//    /* create new folder test */
    @Test
    fun testCreateNewFolder_ok(){
        val newFolderName: String = "je"

        // Create Server Root Structure
        val loginToken: String = registerAndLogin()
        val rootToken: String = fileService.findRootToken(loginToken).rootToken

        val createFolderRequestDTO: CreateFolderRequestDTO = CreateFolderRequestDTO(
            parentFolderToken = rootToken,
            newFolderName = newFolderName
        )
        val content: String = objectMapper.writeValueAsString(createFolderRequestDTO)

        // Perform
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/navi/folder")
                .header("X-AUTH-TOKEN", loginToken)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(content)
        ).andExpect { status(HttpStatus.NO_CONTENT) }
            .andDo(MockMvcResultHandlers.print())
            .andDo{
                assertThat(it.response.status).isEqualTo(HttpStatus.NO_CONTENT.value())
            }

        // Now Check DB
        gridFSRepository.getMetadataInsideFolder(mockUser.userId, rootToken).also {
            assertThat(it.size).isEqualTo(1)
            assertThat(it[0].fileName).isEqualTo(newFolderName)
        }
    }

    @Test
    fun testCreateNewFolder_ConflictException() {
        val newFolderName: String = "je"

        // Create Server Root Structure
        val loginToken: String = registerAndLogin()
        val rootToken: String = fileService.findRootToken(loginToken).rootToken

        // Create new folder to root directory named $newFolderName
        fileService.createNewFolder(
            userToken = loginToken,
            parentFolderToken = rootToken,
            newFolderName = newFolderName
        )

        // Try to create new folder with the same name $newFolderName
        // This will throw ConflictException
        val createFolderRequestDTO: CreateFolderRequestDTO = CreateFolderRequestDTO(
            parentFolderToken = rootToken,
            newFolderName = newFolderName
        )
        val content: String = objectMapper.writeValueAsString(createFolderRequestDTO)

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/navi/folder")
                .header("X-AUTH-TOKEN", loginToken)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(content)
        ).andExpect { status(HttpStatus.OK) }
            .andDo(MockMvcResultHandlers.print())
            .andDo{
                assertThat(it.response.status).isEqualTo(HttpStatus.CONFLICT.value())
            }
    }
}
