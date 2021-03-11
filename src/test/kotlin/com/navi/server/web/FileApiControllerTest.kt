package com.navi.server.web

import com.navi.server.component.FileConfigurationComponent
import com.navi.server.domain.FileEntity
import com.navi.server.domain.FileRepository
import com.navi.server.dto.FileResponseDTO
import com.navi.server.service.FileService
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.ResponseEntity
import org.springframework.test.context.junit4.SpringRunner
import org.assertj.core.api.Assertions.assertThat;
import org.junit.Before
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity.status
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.io.File
import org.springframework.web.context.WebApplicationContext

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FileApiControllerTest {
    @LocalServerPort
    var port: Int = 0

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Autowired
    lateinit var fileRepository: FileRepository

    @Autowired
    private lateinit var fileConfigurationComponent: FileConfigurationComponent

    @Autowired
    private lateinit var fileService: FileService

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    private lateinit var trashRootObject: File

    private lateinit var mockMvc : MockMvc

    @Before
    fun initEnvironment() {
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
        fileRepository.deleteAll()
    }


    @Test
    fun testFindAllDesc() {
        //insert data
        val fileName = listOf<String>("fileName1", "fileName2", "fileName3", "fileName4")
        fileName.forEach {
            val id = fileRepository.save(FileEntity(fileName = it, fileType = "fileType", mimeType = "text/plain", token = "token", prevToken = "token", lastModifiedTime = 5000, fileCreatedDate = "testCreatedTime", fileSize = "5000"))
        }
        //send api request
        val url = "http://localhost:$port/api/navi/files/list"
        var responseEntity : ResponseEntity<Array<FileResponseDTO>> = restTemplate.getForEntity(url, Array<FileResponseDTO>::class.java)

        //Assert
        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(responseEntity.body.size).isEqualTo(4)
        var i: Int = fileName.size-1
        responseEntity.body.forEach {
            assertThat(it.fileName).isEqualTo(fileName[i--])
        }
    }

    @Test
    fun testFindRootToken() {
        // Get Api
        val url = "http://localhost:$port/api/navi/root-token"
        var responseEntity : ResponseEntity<String> = restTemplate.getForEntity(url, String::class.java)

        // Assert
        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(responseEntity.body).isEqualTo(fileService.getSHA256(fileConfigurationComponent.serverRoot))
    }

    @Test
    fun testFindInsideFiles() {
        // before findInsideFiles Test, Create test file/folder
        // one empty file to root
        val fileName: String = "Testing.txt"
        val fileObject: File = File(fileConfigurationComponent.serverRoot, fileName)
        if (!fileObject.exists()) {
            fileObject.createNewFile()
        }
        saveFileEntityToDB(fileObject.absolutePath, "File", fileConfigurationComponent.serverRoot)

        // Create one empty Folder to root
        val folderName : String = "TestFolder"
        val folderObject: File = File(fileConfigurationComponent.serverRoot, folderName)
        if (!folderObject.exists()) {
            folderObject.mkdir()
        }
        saveFileEntityToDB(folderObject.absolutePath, "Folder", fileConfigurationComponent.serverRoot)

        // Creat 3 Child Files in Folder [folderName]
        val folderPath = folderObject.absolutePath
        val childFiles = listOf<File>(
            File(folderPath, "file1.txt"),
            File(folderPath, "file2.txt"),
            File(folderPath, "file3.txt"),
        )
        childFiles.forEach {
            if (!it.exists()) {
                it.createNewFile()
            }
            saveFileEntityToDB(it.absolutePath, "File", folderPath)
        }

        // in "folderName" folder
        val folderToken = fileService.getSHA256(folderPath)
        val url2 = "http://localhost:$port/api/navi/files/list/${folderToken}"
        var responseEntity2 : ResponseEntity<Array<FileResponseDTO>> = restTemplate.getForEntity(url2, Array<FileResponseDTO>::class.java)

        // Assert
        assertThat(responseEntity2.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(responseEntity2.body.size).isEqualTo(3) //3 files

        var result = responseEntity2.body
        result.forEach {
            assertThat(it.prevToken).isEqualTo(folderToken)
        }
    }

    @Test
    fun invalid_findInsideFiles(){
        // invalid find Inside files : invalid token
        val invalidToken = "token"
        mockMvc.perform(MockMvcRequestBuilders.get("/api/navi/files/list/${invalidToken}"))
            .andExpect { status(HttpStatus.BAD_REQUEST) }
            .andDo(MockMvcResultHandlers.print())
    }

    @Test
    fun testFileUpload(){
        // Create one test Folder to root
        val folderName : String = "Upload"
        val folderObject: File = File(fileConfigurationComponent.serverRoot, folderName)
        if (!folderObject.exists()) {
            folderObject.mkdir()
        }

        saveFileEntityToDB(folderObject.absolutePath, "Folder")
        val folderObjectToken = fileService.getSHA256(folderObject.absolutePath)

        // Make uploadFile
        val uploadFileName = "uploadTest-api.txt"
        val uploadFileContent = "test upload API!"
        val multipartFile = MockMultipartFile("uploadFile", uploadFileName, "text/plain", uploadFileContent.toByteArray())
        val uploadFolderPath = MockMultipartFile("uploadPath", "uploadPath", "text/plain", folderObjectToken.toByteArray())

        // Perform
        mockMvc.perform(
            MockMvcRequestBuilders.multipart("/api/navi/files")
                .file(multipartFile)
                .file(uploadFolderPath)
        ).andExpect { status(HttpStatus.OK) }
            .andDo(MockMvcResultHandlers.print())

        // Assert
        val targetFile = File(folderObject.absolutePath, uploadFileName)

        val resultFromDB = fileRepository.findAll().find { it.fileName == targetFile.absolutePath }
        resultFromDB?.let {
            assertThat(resultFromDB.fileName).isEqualTo(targetFile.absolutePath)
        } ?: throw Exception("ERROR:: no $uploadFileName")

        val resultFromServer = folderObject.listFiles().find { it.isFile && it.absolutePath == targetFile.absolutePath }
        resultFromServer?.let {
            assertThat(resultFromServer.absolutePath).isEqualTo(targetFile.absolutePath)
        } ?: throw Exception("ERROR:: no ${targetFile.absolutePath}")

    }

    @Test
    fun invalid_fileUpload(){
        // make MultipartFile for test
        val uploadFileName = "uploadTest-service.txt"
        var uploadFileContent = "file upload test file!".toByteArray()
        val multipartFile = MockMultipartFile(
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
        ).andExpect { status(HttpStatus.BAD_REQUEST) }
            .andDo(MockMvcResultHandlers.print())

        // Why do i need this?
        saveFileEntityToDB(fileService.rootPath!!, "Folder")

        // invalid upload test 2 : invalid multipartFile (IOException)
        val multipartFile2 = MockMultipartFile(
            "uploadFile", "".toByteArray()
        )
        val uploadFolderPath2 = MockMultipartFile("uploadPath", "uploadPath", "text/plain", fileService.rootToken!!.toByteArray())
        // Perform
        mockMvc.perform(
            MockMvcRequestBuilders.multipart("/api/navi/files")
                .file(multipartFile2)
                .file(uploadFolderPath2)
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
        ).andExpect { status(HttpStatus.INTERNAL_SERVER_ERROR) }
            .andDo(MockMvcResultHandlers.print())
    }

    @Test
    fun testFileDownload(){
        // Make one test file to root
        val fileName: String = "downloadTest-api.txt"
        val fileObject: File = File(fileConfigurationComponent.serverRoot, fileName)
        val fileContent = "test download API!"
        if (!fileObject.exists()) {
            fileObject.createNewFile()
        }
        fileObject.writeText(fileContent);

        saveFileEntityToDB(fileObject.absolutePath, "File")
        val targetToken = fileService.getSHA256(fileObject.absolutePath)

        // Perform
        val url = "http://localhost:$port/api/navi/files/${targetToken}"
        val responseEntity = restTemplate.getForEntity(url, Resource::class.java)
        val resource : Resource? = responseEntity.body

        // Assert
        val contentDisposition : String? = responseEntity.headers.get(HttpHeaders.CONTENT_DISPOSITION)!!.get(0)
        contentDisposition?.let {
            val resultFileName = it.split("=")[1]
            assertThat(resultFileName.substring(1, resultFileName.length-1)).isEqualTo(fileName)
        } ?: throw Exception("File Name mismatch OR No File Name in ContentDisposition")

        resource?.let {
            val resultContent = resource.inputStream.readBytes().toString(Charsets.UTF_8)
            assertThat(resultContent).isEqualTo(fileContent)
        } ?: throw Exception("No File : ${fileObject.absolutePath}")
    }

    @Test
    fun quotationMarkedFileNameUpload(){
        // Create one test Folder to root
        val folderName : String = "Upload"
        val folderObject: File = File(fileConfigurationComponent.serverRoot, folderName)
        if (!folderObject.exists()) {
            folderObject.mkdir()
        }

        saveFileEntityToDB(folderObject.absolutePath, "Folder")

        // insert quotation mark
        val folderObjectToken = "\"" + fileService.getSHA256(folderObject.absolutePath) + "\""

        // Make uploadFile
        val uploadFileName = "quotationTest.txt"
        val uploadFileContent = "test upload API!"
        val multipartFile = MockMultipartFile("uploadFile", uploadFileName, "text/plain", uploadFileContent.toByteArray())
        val uploadFolderPath = MockMultipartFile("uploadPath", "uploadPath", "text/plain", folderObjectToken.toByteArray())

        // Perform
        mockMvc.perform(
            MockMvcRequestBuilders.multipart("/api/navi/files")
                .file(multipartFile)
                .file(uploadFolderPath)
        ).andExpect { status(HttpStatus.OK) }
            .andDo(MockMvcResultHandlers.print())

        // Assert
        val targetFile = File(folderObject.absolutePath, uploadFileName)

        val resultFromDB = fileRepository.findAll().find { it.fileName == targetFile.absolutePath }
        resultFromDB?.let {
            assertThat(resultFromDB.fileName).isEqualTo(targetFile.absolutePath)
        } ?: throw Exception("ERROR:: no $uploadFileName")

        val resultFromServer = folderObject.listFiles().find { it.isFile && it.absolutePath == targetFile.absolutePath }
        resultFromServer?.let {
            assertThat(resultFromServer.absolutePath).isEqualTo(targetFile.absolutePath)
        } ?: throw Exception("ERROR:: no ${targetFile.absolutePath}")

    }

    @Test
    fun invalid_FileDownload() {
        val fileName: String = "invalidDownloadTest-api.txt"
        val fileObject: File = File(fileConfigurationComponent.serverRoot, fileName)

        // invalid file Download 1 : invalid token
        var targetToken = fileService.getSHA256(fileObject.absolutePath) // invalid path (no such file in server DB)
        val url = "http://localhost:$port/api/navi/files/${targetToken}"
        val responseEntity = restTemplate.getForEntity(url, Resource::class.java)

        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.NOT_FOUND)


        // invalid file Download 2 : FileNotFoundException (no such file at server)
        // save to (only) DB
        fileRepository.save(
            FileEntity(
                fileName = fileObject.absolutePath,
                fileType = "type",
                mimeType = "text/plain",
                token = targetToken,
                prevToken = "prevToken",
                lastModifiedTime = 1L,
                fileCreatedDate = "now",
                fileSize = "size"
            )
        )
        val responseEntity2 = restTemplate.getForEntity(url, Resource::class.java)
        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun return_serverRootDoesNotExists_when_root_token_is_null() {
        val url: String = "http://localhost:$port/api/navi/root-token"
        // There might be no change to be NULL, but we let them[probably hacker?]
        val backupToken: String? = fileService.rootToken
        fileService.rootToken = null

        val responseEntity: ResponseEntity<String> = restTemplate.getForEntity(url, String::class.java)

        // Assert
        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        //assertThat(responseEntity.body).isEqualTo("serverRoot does not exist!")

        // restore token
        fileService.rootToken = backupToken
    }

    fun saveFileEntityToDB(filename: String, fileType: String){
        fileRepository.save(FileEntity(
            fileName = filename,
            fileType = fileType,
            mimeType = "File",
            token = fileService.getSHA256(filename),
            prevToken = "",
            lastModifiedTime = 1L,
            fileCreatedDate = "date",
            fileSize = "size"
        ))
    }

    fun saveFileEntityToDB(filename: String, fileType: String, prevToken: String){
        fileRepository.save(FileEntity(
            fileName = filename,
            fileType = fileType,
            mimeType = "File",
            token = fileService.getSHA256(filename),
            prevToken = fileService.getSHA256(prevToken),
            lastModifiedTime = 1L,
            fileCreatedDate = "date",
            fileSize = "size"
        ))
    }
}
