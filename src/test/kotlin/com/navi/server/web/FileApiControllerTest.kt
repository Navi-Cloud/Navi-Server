package com.navi.server.web

import com.navi.server.component.FileConfigurationComponent
import com.navi.server.domain.FileObject
import com.navi.server.domain.user.User
import com.navi.server.domain.user.UserTemplateRepository
import com.navi.server.dto.FileResponseDTO
import com.navi.server.security.JWTTokenProvider
import com.navi.server.service.FileService
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.test.context.junit4.SpringRunner
import org.assertj.core.api.Assertions.assertThat;
import org.assertj.core.api.Assertions.fail
import org.junit.Before
import org.springframework.core.io.Resource
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
        val mockUser: User = User(
            userName = "KangDroid",
            userEmail = "",
            userPassword = "",
            roles = setOf("USER")
        )
        // Register
        userTemplateRepository.save(
            User(
                userName = "KangDroid",
                userEmail = "",
                userPassword = "",
                roles = setOf("USER")
            )
        )

        // Token
        return jwtTokenProvider.createToken(mockUser.userName, mockUser.roles.toList())
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

        mockMvc.perform(MockMvcRequestBuilders.get("/api/navi/root-token")
            .header("X-AUTH-TOKEN", loginToken))
            .andExpect { status(HttpStatus.OK) }
            .andDo(MockMvcResultHandlers.print())

        // Assert
        println(responseEntity)
        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)

        //assertThat(responseEntity.body).isEqualTo(fileService.getSHA256(fileConfigurationComponent.serverRoot))
    }
    @Test
    fun testFindAllDesc_ok() {
        val loginToken: String = registerAndLogin()
        fileConfigurationComponent.initStructure()

//        //insert data
//        val fileName = listOf<String>("fileName1", "fileName2", "fileName3", "fileName4")
//        fileName.forEach {
//            val result = fileRepository.save(FileEntity(fileName = it, fileType = "fileType", mimeType = "text/plain", token = "token", prevToken = "token", lastModifiedTime = 5000, fileCreatedDate = "testCreatedTime", fileSize = "5000"))
//        }

        //send api request
        val url = "http://localhost:$port/api/navi/files/list"
        val headers: HttpHeaders = HttpHeaders()
        headers.add("X-AUTH-TOKEN", loginToken)
        //var responseEntity : ResponseEntity<Array<FileResponseDTO>> = restTemplate.exchange(url, HttpMethod.GET, HttpEntity<String>(headers), Array<FileResponseDTO>::class.java)
        runCatching { restTemplate.exchange(url, HttpMethod.GET, HttpEntity<String>(headers), Array<FileResponseDTO>::class.java) }
            .onSuccess {
                println(it.body.size)
            }.onFailure {
                println(it.stackTraceToString())
                fail("this ...is not ok..^%^")
            }

        //var responseEntity : ResponseEntity<Array<FileResponseDTO>> = restTemplate.getForEntity(url, Array<FileResponseDTO>::class.java)

        //Assert
        //assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
        //assertThat(responseEntity.body.size).isEqualTo(1L)
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
        var responseEntity2 : ResponseEntity<Array<FileResponseDTO>> = restTemplate.exchange(url2, HttpMethod.GET, HttpEntity("",headers), Array<FileResponseDTO>::class.java)

        // Assert
        assertThat(responseEntity2.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(responseEntity2.body!!.size).isEqualTo(1L) //3 files

        var result = responseEntity2.body
    }

    @Test
    fun invalid_findInsideFiles_404_no_file(){
        val loginToken: String = registerAndLogin()
        // invalid find Inside files : invalid token
        val invalidToken = "token"
        mockMvc.perform(MockMvcRequestBuilders.get("/api/navi/files/list/${invalidToken}")
            .header("X-AUTH-TOKEN", loginToken))
            .andExpect { status(HttpStatus.BAD_REQUEST) }
            .andDo(MockMvcResultHandlers.print())
            .andDo {
                assertThat(it.response.status).isEqualTo(HttpStatus.NOT_FOUND)
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
            uploadFileName, uploadFileName, "text/plain", uploadFileContent
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
            uploadFileName, uploadFileName, "text/plain", uploadFileContent
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
            uploadFileName, uploadFileName, "text/plain", uploadFileContent
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
        ).andExpect { status(HttpStatus.BAD_REQUEST) }
            .andDo(MockMvcResultHandlers.print())

        // Why do i need this?
        //saveFileEntityToDB(fileService.rootPath!!, "Folder")

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


        // Perform
        val url = "http://localhost:$port/api/navi/files/${fileObject.token}"
        // TODO: add User token (header)
        val responseEntity = restTemplate.getForEntity(url, Resource::class.java)
        val resource : Resource? = responseEntity.body

        // Assert
        val contentDisposition : String? = responseEntity.headers.get(HttpHeaders.CONTENT_DISPOSITION)!!.get(0)
        contentDisposition?.let {
            val resultFileName = it.split("=")[1]
            assertThat(resultFileName.substring(1, resultFileName.length-1)).isEqualTo(fileObject.fileName)
        } ?: throw Exception("File Name mismatch OR No File Name in ContentDisposition")
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
        // TODO: add user login token
        val url = "http://localhost:$port/api/navi/files/${fileObject.token}"
        val responseEntity = restTemplate.getForEntity(url, Resource::class.java)
        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.NOT_FOUND)

        // invalid file Download 2 : FileNotFoundException (no such file at server)
        // save to (only) DB
        val user: User = userTemplateRepository.findByUserName("KangDroid")
        user.fileList.add(fileObject)
        userTemplateRepository.save(user)

        // TODO: add user login token
        val responseEntity2 = restTemplate.getForEntity(url, Resource::class.java)
        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

//    @Test
//    fun return_serverRootDoesNotExists_when_root_token_is_null() {
//        val url: String = "http://localhost:$port/api/navi/root-token"
//        // There might be no change to be NULL, but we let them[probably hacker?]
//        val backupToken: String? = fileService.rootToken
//        fileService.rootToken = null
//
//        val responseEntity: ResponseEntity<String> = restTemplate.getForEntity(url, String::class.java)
//
//        // Assert
//        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
//        //assertThat(responseEntity.body).isEqualTo("serverRoot does not exist!")
//
//        // restore token
//        fileService.rootToken = backupToken
//    }
//
//    fun saveFileEntityToDB(filename: String, fileType: String){
//        fileRepository.save(FileEntity(
//            fileName = filename,
//            fileType = fileType,
//            mimeType = "File",
//            token = fileService.getSHA256(filename),
//            prevToken = "",
//            lastModifiedTime = 1L,
//            fileCreatedDate = "date",
//            fileSize = "size"
//        ))
//    }
//
//    fun saveFileEntityToDB(filename: String, fileType: String, prevToken: String){
//        fileRepository.save(FileEntity(
//            fileName = filename,
//            fileType = fileType,
//            mimeType = "File",
//            token = fileService.getSHA256(filename),
//            prevToken = fileService.getSHA256(prevToken),
//            lastModifiedTime = 1L,
//            fileCreatedDate = "date",
//            fileSize = "size"
//        ))
//    }
}
