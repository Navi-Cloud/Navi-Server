package com.navi.server.web

import com.navi.server.component.FileConfigurationComponent
import com.navi.server.domain.FileObject
import com.navi.server.domain.user.User
import com.navi.server.domain.user.UserTemplateRepository
import com.navi.server.dto.FileResponseDTO
import com.navi.server.dto.LoginRequest
import com.navi.server.dto.LoginResponse
import com.navi.server.dto.UserRegisterRequest
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
import org.springframework.http.*
import org.springframework.http.ResponseEntity.status
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.io.File
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.nio.file.Path
import java.nio.file.Paths

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FileApiControllerTest {
    @LocalServerPort
    var port: Int = 0

    @Autowired
    lateinit var restTemplate: TestRestTemplate

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

    private lateinit var trashRootObject: File

    private lateinit var mockMvc : MockMvc

    @Before
    fun initEnvironment() {
        fileConfigurationComponent.serverRoot = File(System.getProperty("java.io.tmpdir"), "naviServerTesting").absolutePath
        // Create trash directory
        trashRootObject = File(fileConfigurationComponent.serverRoot)
        trashRootObject.mkdir()
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
    }

    @After
    fun destroyEnvironment() {
        if (trashRootObject.exists()) {
            trashRootObject.deleteRecursively()
        }
        userTemplateRepository.clearAll()
    }

    private fun registerAndLogin(): String {
        val mockUser: UserRegisterRequest = UserRegisterRequest(
            userName = "KangDroid",
            userPassword = "password",
            userEmail = "test@test.com"
        )

        userService.registerUser(
            mockUser
        )

        val loginResponseDto: ResponseEntity<LoginResponse> = userService.loginUser(
                LoginRequest(
                    userName = mockUser.userName,
                    userPassword = mockUser.userPassword
                )
        )

        return loginResponseDto.body.userToken
    }

    @Test
    fun testFindRootToken() {
        val loginToken: String = registerAndLogin()
        fileConfigurationComponent.initStructure()

        // Get Api
        val url = "http://localhost:$port/api/navi/root-token"
        val headers: HttpHeaders = HttpHeaders()
        headers.add("X-AUTH-TOKEN", loginToken)
        var responseEntity : ResponseEntity<String> = restTemplate.exchange(url, HttpMethod.GET, HttpEntity<Void>(headers), String::class.java)

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/navi/root-token")
                .header("X-AUTH-TOKEN", loginToken)
        ).andExpect { status(HttpStatus.OK) }
            .andDo(MockMvcResultHandlers.print())
            .andDo{
                assertThat(it.response.status).isEqualTo(HttpStatus.OK.value())
                assertThat(it.response.contentAsString).isEqualTo(fileService.getSHA256("/"))
            }
        // Assert
        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(responseEntity.body).isEqualTo(fileService.getSHA256("/"))
    }

    @Test
    fun testFindAllDesc_ok() {
        val loginToken: String = registerAndLogin()
        fileConfigurationComponent.initStructure()

        //send api request
        val url = "http://localhost:$port/api/navi/files/list"
        val headers: HttpHeaders = HttpHeaders()
        headers.add("X-AUTH-TOKEN", loginToken)
        val responseEntity : ResponseEntity<Array<FileResponseDTO>> = restTemplate.exchange(
            url, HttpMethod.GET, HttpEntity<Void>(headers), Array<FileResponseDTO>::class.java
        )

        //Assert
        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(responseEntity.body.size).isEqualTo(1L)
    }

    @Test
    fun testFindInsideFiles_ok() {
        val loginToken: String = registerAndLogin()
        File(fileConfigurationComponent.serverRoot, "KangDroid").mkdir()
        val path: Path = Paths.get(fileConfigurationComponent.serverRoot, "KangDroid", "test.txt")
        val fileObject: File = path.toFile()
        fileObject.writeText("test")
        fileConfigurationComponent.initStructure()

        // in "folderName" folder
        val folderToken = fileService.getSHA256("/")
        val url2 = "http://localhost:$port/api/navi/files/list/${folderToken}"
        val headers: HttpHeaders = HttpHeaders()
        headers.add("X-AUTH-TOKEN", loginToken)
        var responseEntity2 : ResponseEntity<Array<FileResponseDTO>> = restTemplate.exchange(
            url2, HttpMethod.GET, HttpEntity<Void>(headers), Array<FileResponseDTO>::class.java
        )

        // Assert
        assertThat(responseEntity2.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(responseEntity2.body!!.size).isEqualTo(1L)
    }

    @Test
    fun invalid_findInsideFiles_404_no_file(){
        val loginToken: String = registerAndLogin()

        // invalid find Inside files : invalid token
        val invalidToken = "token"
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/navi/files/list/${invalidToken}")
                .header("X-AUTH-TOKEN", loginToken)
        ).andExpect { status(HttpStatus.NOT_FOUND) }
            .andDo(MockMvcResultHandlers.print())
            .andDo{
                assertThat(it.response.status).isEqualTo(HttpStatus.NOT_FOUND.value())
            }
    }

    // test upload
    @Test
    fun testFileUpload_ok(){
        // Create Server Root Structure
        val loginToken: String = registerAndLogin()
        fileConfigurationComponent.initStructure()

        // make uploadFile
        val uploadFileName: String = "uploadTest-service.txt"
        val uploadFileContent: ByteArray = "file upload test file!".toByteArray()
        val multipartFile: MockMultipartFile = MockMultipartFile(
            "uploadFile", uploadFileName, "text/plain", uploadFileContent
        )

        // file upload
        val uploadFolderToken = fileService.getSHA256("/") // Upload to user's root
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
        val userName: String = jwtTokenProvider.getUserPk(loginToken)
        val uploadedFileObject: File =
            Paths.get(fileConfigurationComponent.serverRoot, userName, uploadFileName).toFile()
        assertThat(uploadedFileObject.exists()).isEqualTo(true)

        // Now Check DB
        val user: User = userTemplateRepository.findByUserName(userName)
        val fileList: MutableList<FileObject> = user.fileList
        assertThat(fileList.size).isEqualTo(2L)
        assertThat(fileList[1].fileName).isEqualTo("/$uploadFileName")
    }

    @Test
    fun testFileUpload_quotationMarkedFileName_ok(){
        // Create Server Root Structure
        val loginToken: String = registerAndLogin()
        fileConfigurationComponent.initStructure()

        // make uploadFile
        val uploadFileName: String = "uploadTest-service.txt"
        val uploadFileContent: ByteArray = "file upload test file!".toByteArray()
        val multipartFile: MockMultipartFile = MockMultipartFile(
            "uploadFile", uploadFileName, "text/plain", uploadFileContent
        )

        // file upload
        // insert quotation mark
        val uploadFolderToken = "\"" + fileService.getSHA256("/") + "\""  // Upload to user's root
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
        val userName: String = jwtTokenProvider.getUserPk(loginToken)
        val uploadedFileObject: File =
            Paths.get(fileConfigurationComponent.serverRoot, userName, uploadFileName).toFile()
        assertThat(uploadedFileObject.exists()).isEqualTo(true)

        // Now Check DB
        val user: User = userTemplateRepository.findByUserName(userName)
        val fileList: MutableList<FileObject> = user.fileList
        assertThat(fileList.size).isEqualTo(2L)
        assertThat(fileList[1].fileName).isEqualTo("/$uploadFileName")
    }

    @Test
    fun invalid_fileUpload(){
        // Create Server Root Structure
        val loginToken: String = registerAndLogin()
        fileConfigurationComponent.initStructure()

        // make uploadFile
        val uploadFileName: String = "uploadTest-service.txt"
        val uploadFileContent: ByteArray = "file upload test file!".toByteArray()
        val multipartFile: MockMultipartFile = MockMultipartFile(
            "uploadFile", uploadFileName, "text/plain", uploadFileContent
        )

        // invalid upload test 1 : invalid token
        val invalidToken = "token"
        val uploadFolderPath = MockMultipartFile("uploadPath", "uploadPath", "text/plain", invalidToken.toByteArray())
        // Perform
        mockMvc.perform(
            MockMvcRequestBuilders.multipart("/api/navi/files")
                .file(multipartFile)
                .file(uploadFolderPath)
                .header("X-AUTH-TOKEN", loginToken)
        ).andExpect { status(HttpStatus.NOT_FOUND) }
            .andDo(MockMvcResultHandlers.print())
            .andDo{
                assertThat(it.response.status).isEqualTo(HttpStatus.NOT_FOUND.value())
            }


        // invalid upload test 2 : invalid multipartFile (IOException)
        val multipartFile2 = MockMultipartFile(
            "uploadFile", "".toByteArray()
        )
        val uploadFolderPath2 = MockMultipartFile("uploadPath", "uploadPath", "text/plain", fileService.getSHA256("/").toByteArray())
        // Perform
        mockMvc.perform(
            MockMvcRequestBuilders.multipart("/api/navi/files")
                .file(multipartFile2)
                .file(uploadFolderPath2)
                .header("X-AUTH-TOKEN", loginToken)
        ).andExpect { status(HttpStatus.INTERNAL_SERVER_ERROR) }
            .andDo(MockMvcResultHandlers.print())
            .andDo{
                assertThat(it.response.status).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value())
            }


        // invalid upload test 3 : invalid multipartFile
        val multipartFile3 = MockMultipartFile(
            "uploadFile", "\"", "test", "".toByteArray()
        )
        // Perform
        mockMvc.perform(
            MockMvcRequestBuilders.multipart("/api/navi/files")
                .file(multipartFile3)
                .file(uploadFolderPath2)
                .header("X-AUTH-TOKEN", loginToken)
        ).andExpect { status(HttpStatus.INTERNAL_SERVER_ERROR) }
            .andDo(MockMvcResultHandlers.print())
            .andDo{
                assertThat(it.response.status).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value())
            }
    }

    // test fileDownload
    @Test
    fun testFileDownload_ok(){
        // Let
        val fileObject: FileObject = FileObject(
            fileName = "test_file",
            fileType = "test_type",
            mimeType = "mime_type",
            token = "test_token",
            prevToken = "prev_token",
            lastModifiedTime = System.currentTimeMillis(),
            fileCreatedDate = "date",
            fileSize = "5m"
        )

        val loginToken: String = registerAndLogin()
        val user: User = userTemplateRepository.findByUserName("KangDroid")
        user.fileList.add(fileObject)
        userTemplateRepository.save(user)
        // Write some texts
        Paths.get(fileConfigurationComponent.serverRoot, "KangDroid").toFile().mkdirs()
        val file: File = Paths.get(fileConfigurationComponent.serverRoot, "KangDroid", fileObject.fileName).toFile()
        file.writeText("Test!")


        // Perform and Assert
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/navi/files/${fileObject.token}")
                .header("X-AUTH-TOKEN", loginToken)
        ).andExpect { status(HttpStatus.OK) }
            .andDo(MockMvcResultHandlers.print())
            .andDo{
                assertThat(it.response.status).isEqualTo(HttpStatus.OK.value())
                assertThat(it.response.contentAsString).isEqualTo("Test!")
            }
    }

    @Test
    fun invalid_FileDownload_404_NotFound() {
        val loginToken: String = registerAndLogin()

        // Let
        val fileObject: FileObject = FileObject(
            fileName = "test_file",
            fileType = "test_type",
            mimeType = "mime_type",
            token = "test_token",
            prevToken = "prev_token",
            lastModifiedTime = System.currentTimeMillis(),
            fileCreatedDate = "date",
            fileSize = "5m"
        )

        // invalid file Download 1 : invalid token
        val url = "http://localhost:$port/api/navi/files/${fileObject.token}"
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/navi/files/${fileObject.token}")
                .header("X-AUTH-TOKEN", loginToken)
        ).andExpect { status(HttpStatus.NOT_FOUND) }
            .andDo(MockMvcResultHandlers.print())
            .andDo{
                assertThat(it.response.status).isEqualTo(HttpStatus.NOT_FOUND.value())
            }

        // invalid file Download 2 : FileNotFoundException (no such file at server)
        // save to (only) DB
        val user: User = userTemplateRepository.findByUserName("KangDroid")
        user.fileList.add(fileObject)
        userTemplateRepository.save(user)

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/navi/files/${fileObject.token}")
                .header("X-AUTH-TOKEN", loginToken)
        ).andExpect { status(HttpStatus.NOT_FOUND) }
            .andDo(MockMvcResultHandlers.print())
            .andDo{
                assertThat(it.response.status).isEqualTo(HttpStatus.NOT_FOUND.value())
            }
    }
}
