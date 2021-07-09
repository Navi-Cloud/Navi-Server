package com.navi.server.web

import com.fasterxml.jackson.databind.ObjectMapper
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
import org.springframework.boot.test.web.client.getForEntity
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.gridfs.GridFsTemplate
import org.springframework.http.*
import org.springframework.http.ResponseEntity.status
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

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

    private fun encodeString(inputString: String): String = URLEncoder.encode(inputString, StandardCharsets.UTF_8.toString())

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
        val url2 = "http://localhost:$port/api/navi/files/list/${encodeString(rootToken)}"
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
    @Test
    fun testFileDownload_ok(){
        val loginToken: String = registerAndLogin()
        val rootToken: String = fileService.findRootToken(loginToken).rootToken

        // make uploadFile
        val uploadFileName: String = "uploadTest-service.txt"
        val uploadFileContent: ByteArray = "file upload test file!".toByteArray()
        val multipartFile: MockMultipartFile = MockMultipartFile(
            uploadFileName, uploadFileName, "text/plain", uploadFileContent
        )
        val fileObjectUploaded: FileObject = fileService.fileUpload(
            userToken = loginToken,
            uploadFolderToken = rootToken,
            files = multipartFile
        )

        // Perform and Assert
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/navi/files?token=${encodeString(fileObjectUploaded.token)}&prevToken=${encodeString(fileObjectUploaded.prevToken)}")
                .header("X-AUTH-TOKEN", loginToken)
        ).andExpect { status(HttpStatus.OK) }
            .andDo(MockMvcResultHandlers.print())
            .andDo{
                assertThat(it.response.status).isEqualTo(HttpStatus.OK.value())
            }
    }

    /* create new folder test */
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

    @Test
    fun is_removeFile_with_file_works_well() {
        // First upload
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

        // Get Information
        val targetFileObject: FileObject =
            gridFSRepository.getMetadataInsideFolder(mockUser.userId, rootToken)[0]

        mockMvc.perform(
            MockMvcRequestBuilders.delete("/api/navi/files/${encodeString(targetFileObject.prevToken)}/${encodeString(targetFileObject.token)}")
                .header("X-AUTH-TOKEN", loginToken)
        ).andExpect { status(HttpStatus.NO_CONTENT) }

        // Check DB
        val rootFolderList: List<FileObject> =
            gridFSRepository.getMetadataInsideFolder(mockUser.userId, rootToken)
        assertThat(rootFolderList.size).isEqualTo(0)
    }

    @Test
    fun is_removeFile_with_folder_works_well() {
        // Upload first
        val userToken: String = registerAndLogin()
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

        mockMvc.perform(
            MockMvcRequestBuilders.delete("/api/navi/files/${encodeString(folderObject.prevToken)}/${encodeString(folderObject.token)}")
                .header("X-AUTH-TOKEN", userToken)
        ).andExpect { status(HttpStatus.NO_CONTENT) }

        // Check DB
        val rootFolderList: List<FileObject> =
            gridFSRepository.getMetadataInsideFolder(mockUser.userId, rootToken)
        assertThat(rootFolderList.size).isEqualTo(0)
    }

    @Test
    fun is_searchFile_works_well() {
        // Upload first
        val userToken: String = registerAndLogin()
        val rootToken: String = fileService.findRootToken(userToken).rootToken

        // make uploadFile
        val uploadFileName: String = "test"
        val uploadFileContent: ByteArray = "file upload test file!".toByteArray()
        val multipartFile: MockMultipartFile = MockMultipartFile(
            uploadFileName, uploadFileName, "text/plain", uploadFileContent
        )

        val responseFileObject: FileObject = fileService.fileUpload(
            userToken = userToken,
            uploadFolderToken = rootToken,
            files = multipartFile
        )

        // Get Api
        val encodedName: String = URLEncoder.encode(responseFileObject.fileName, StandardCharsets.UTF_8.toString())
        val url = "http://localhost:${port}/api/navi/search?searchParam=${encodedName}"
        val headers: HttpHeaders = HttpHeaders().apply {
            add("X-AUTH-TOKEN", userToken)
        }

        restTemplate.exchange(url, HttpMethod.GET, HttpEntity<Void>(headers), Array<FileObject>::class.java).also {
            assertThat(it.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(it.body!!.size).isEqualTo(1)
            assertThat(it.body[0].fileName).isEqualTo(uploadFileName)
        }
    }
}
